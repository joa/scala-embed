scala-embed
===========

The embed macro allows you to embed content of a file directly into Scala code.

```
import embed._
import scala.xml._

val sql = embed[String]("statement.sql")
val xml = embed[Elem]("config.xml")
val arr = embed[Array[Byte]]("data")
```

The path is always relative to the compilation unit. `embed[String]("file.txt")` for `.\src\main\scala\foo\Bar.scala` will search for `.\src\main\scala\foo\file.txt`.
