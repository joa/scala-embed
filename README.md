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

The path is always relative to the compilation unit. `embed[String]("file.txt")` inside `.\src\main\scala\foo\Bar.scala` will search for `.\src\main\scala\foo\file.txt`.

# Examples
## String
Given the file hello_world.txt contains the String "Hello World"

```
val message = embed[String]("hello_world.txt")
println(message)
```

will expand to

```
val message = "Hello World"
println(message)
```

## Array[Byte]
Given the file data.raw contains three bytes 0xff, 0x80, 0x00

```
val data = embed[Array[Byte]]("data.raw")
for { value <- data } {
  println(value)
}
```

will expand to

```
val data = {
  val result = new Array[Byte](3)
  result(0) = 0xff
  result(1) = 0x80
  result(2) = 0x00
  result
}

for { value <- data } {
  println(value)
}
```

## XML
Given the file config.xml contains "<config><port>8080</port></config>"

```
val config = embed[scala.xml.Elem]("config.xml")
println(config)
```

will expand to

```
val config = scala.xml.XML.loadString("<config><port>8080</port></config>")
println(config)
```

The XML data will be checked for correctness at compile.
