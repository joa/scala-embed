package object embed {
  import scala.language.experimental.macros

  def embed[T](path: String): T = macro Embed.embedImpl[T]

  private[this] object Embed {
    import scala.reflect.macros.Context

    def embedImpl[T : c.WeakTypeTag](c: Context)(path: c.Expr[String]): c.Expr[T] = {
      import c.universe._
      import java.io.File

      val pos = path.tree.pos
      val file = pos.source.file.file.getAbsoluteFile
      val currentDirectory = file.getParentFile

      def contentOf(path: String) = {
        import scala.io.Source

        val source = Source.fromFile(new File(currentDirectory, path))
        val content = source.mkString
        source.close()

        content
      }

      def const[T](value: T) = Literal(Constant(value))

      val Literal(Constant(pathConstant: String)) = path.tree
      val tpe = implicitly[c.WeakTypeTag[T]].tpe

      c.Expr[T](tpe match {
        case t if t =:= typeOf[Nothing] =>
          // We do not know which type we should expand to so
          // tell the user about how to call the macro.

          sys.error(
            "Please call the embed[T] macro with its concrete type.\n"+
            "  val usersStmt: String = embed(\"users.sql\") // ko!\n"+
            "  val usersStmt = embed[String](\"users.sql\") // ok!\n"+
            "  val usersStmt: String = embed[String](\"users.sql\") // ok!")

        case t if t =:= typeOf[String] => {
          // Embed a String constant into the code.
          const(contentOf(pathConstant))
        }

        case t if t =:= typeOf[Array[Byte]] => {
          // Embed an array of bytes into the code.
          
          import java.io.FileInputStream

          val input = new FileInputStream(new File(currentDirectory, pathConstant))
          val bytes = Array.ofDim[Byte](input.available)

          input.read(bytes)
          input.close()

          // Create an array and fill it with constant values.
          // This expands into:
          //
          //   {
          //     val array = new Array[Byte](`length`)
          //     array.update(0, `byte 0`)
          //     array.update(1, `byte 1`)
          //     [...]
          //     array.update(n, `byte n`)
          //     array
          //   }
          //

          val arrayLength = c.Expr[Int](const(bytes.length))
          val valName = newTermName("array")
          val update = newTermName("update")
          val valDef = ValDef(Modifiers(), valName, TypeTree(typeOf[Array[Byte]]), reify(new Array[Byte](arrayLength.splice)).tree)
          
          val fill = 
            for { i <- 0 until bytes.length } yield {
              Apply(Select(Ident(valName), update), List(const(i), const(bytes(i))))
            }

          Block((valDef +: fill :+ Ident(valName)):_*)
        }

        case t if t =:= typeOf[scala.xml.Elem] => {
          // Embed a scala.xml.Elem object into the code.

          import scala.xml._

          // XML handling is so broken that I will not try
          // to make a compile-time implementation here.
          //
          // If future Scala versions decide to fix it this would
          // in theory still work since XML.loadString(...) could
          // be supported as well.
          // 
          // Instead this is a weak verification step. Given the
          // same Scala version at compile- and run-time it should
          // be as safe as expecting that every machine is the same.

          val xmlData = contentOf(pathConstant)

          try {
            XML.loadString(xmlData)
          } catch {
            case error: Exception => 
              sys.error("Cannot embed malformed XML.\n\n"+xmlData)
          }        

          Apply(Select(Select(Select(Ident(newTermName("scala")), "xml"), "XML"), newTermName("loadString")), List(const(xmlData)))
        }

        case unknown => {
          // Unsupported type. In that case a pull-request could
          // bring closure.

          sys.error(
            "The given type \""+unknown+"\" is not supported.\n"+
            "  embed[String](\"file.txt\") for string literals.\n"+
            "  embed[Array[Byte]](\"picture.jpg\") for binary data.\n"+
            "  embed[scala.xml.Elem](\"config.xml\") for XML.\n")
        }
      })
    }
  }
}