scala-embed
===========

The embed macro allows you to embed content of a file right into the Scala code.

```
import embed._
import scala.xml._

val sql = embed[String]("statement.sql")
val xml = embed[Elem]("config.xml")
val arr = embed[Array[Byte]]("data")
```
