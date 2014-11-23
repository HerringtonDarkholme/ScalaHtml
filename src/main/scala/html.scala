import scala.language.dynamics
import scala.Dynamic
import scala.collection.mutable.TreeSet
import scala.collection.mutable.HashMap
import scala.reflect.ClassTag

object HTML {

abstract class Attr[V](val name: String) {
  var value: V = _
  def :=(v: V) = {
    value = v
    this
  }
  override def toString = s"$name=$printVal"
  def printVal: String = "\"" + value + "\""
}

trait Tag extends Dynamic {
  var classes = new TreeSet[String]()
  var attrs = new TreeSet[String]()

  def apply(attrs: Attr[_]*): this.type = {
    for (attr <- attrs) this.attrs += attr.toString
    this
  }
  def selectDynamic(clz: String): this.type = {
    classes += clz
    this
  }
  def applyDynamic(clz: String)(attr: Attr[_]*): this.type = {
    classes += clz
    for (attr <- attrs) this.attrs += attr.toString
    this
  }

  def ::[P[A <: this.type] <: NestTag[A]](parentTag: P[_]) = {
    // compensation for the absence of right to left inference
    val castedParent = parentTag.asInstanceOf[P[this.type]]
    castedParent | this
    castedParent
  }
}

abstract class NestTag[A <: Tag](val name: String) extends Tag {
  var child: A = _

  val defaultval = null.asInstanceOf[A]
  def |(t: A = defaultval): this.type = {
    child = t
    this
  }

  private def printChild: String = {
    if (child == null) return ""
    val innerTag = child.toString.split("\n").map("  " + _).mkString("\n")
    "\n" + innerTag + "\n"
  }
  private def printCls = {
    if (classes.isEmpty) ""
    else " class=\"" + classes.mkString(" ") + "\""
  }

  private def printAttrs = {
    if (attrs.isEmpty) ""
    else " " + attrs.mkString(" ")
  }

  override def toString =
    s"<$name$printCls$printAttrs>$printChild</$name>"
}

trait Companion[T[A <: ChildBound] <: NestTag[A], ChildBound <: Tag] extends Dynamic {
  def selectDynamic[C <: ChildBound](clz: String)(implicit ev: ClassTag[T[C]]) = {
    val node: T[C] = ev.runtimeClass.newInstance.asInstanceOf[T[C]]
    node.selectDynamic(clz)
  }
  def applyDynamic[C <: ChildBound](clz: String)(attrs: Attr[_]*)(implicit ev: ClassTag[T[C]]) = {
    val node: T[C] = ev.runtimeClass.newInstance.asInstanceOf[T[C]]
    node.selectDynamic(clz)
    for (attr <- attrs) node(attr)
    node
  }
  def |[C <: ChildBound](t: C = null)(implicit ev: ClassTag[T[C]]) = {
    val node: T[C] = ev.runtimeClass.newInstance.asInstanceOf[T[C]]
    node | t
  }
  def apply[C <: ChildBound](attrs: Attr[_]*)(implicit ev: ClassTag[T[C]]) = {
    val node: T[C] = ev.runtimeClass.newInstance.asInstanceOf[T[C]]
    for (attr <- attrs) node(attr)
    node
  }
}

object id extends Attr[String]("id")
object width extends Attr[Int]("width")
object action extends Attr[String]("action")
object tpe extends Attr[String]("type")
implicit def attrifyPair(p: (String, String)): Attr[String] =
  new Attr[String](p._1){} := p._2

trait Inline extends Tag
trait Block extends Tag

class Div[T <: Tag] extends NestTag[T]("div") with Block
object div extends Companion[Div, Tag]

class P[T <: Tag] extends NestTag[T]("p") with Block
object p extends Companion[P, Tag]

class Form[T <: Tag] extends NestTag[T]("form") with Block
object form extends Companion[Form, Tag]

class A[T <: Inline] extends NestTag[T]("a") with Inline
object a extends Companion[A, Inline]
class Input[T <: Inline] extends NestTag[T]("input") with Inline
object input extends Companion[Input, Inline]


class Concat[A <: Tag, B <: Tag](a: A, b: B) extends Tag
implicit class PlusTag[A <: Tag](a: A) {
  def +[B <: Tag](b: B) = new Concat(a, b)
}

trait Contains[A <: Tag, C[_ <: Tag] <: NestTag[_], T[_ <: Tag] <: NestTag[_]] {
  type D <: Tag
  def extract(k: C[A]): T[D]
}

case class jQ[A <: Tag, C[_ <: Tag] <: NestTag[_]](c: C[A]) {
  def has[T[_ <: Tag] <: NestTag[_]](implicit ev: Contains[A, C, T]) = ev.extract(c)
}

trait LowPriorityImplicit {
  implicit def htmlEq[A <: Tag, C[_ <: Tag] <: NestTag[_], T[_ <: Tag] <: NestTag[_]](implicit ev: C[A] =:= T[A]) =
    new Contains[A, C, T] {
      type D = A
      def extract(k: C[A]): T[D] = k
    }

  implicit def htmlEq2[
    A <: Tag,
    B[_ <: Tag] <: NestTag[_],
    C[_ <: Tag] <: NestTag[_],
    T[_ <: Tag] <: NestTag[_]](implicit ev: C[B[A]] =:= T[A]) =
    new Contains[B[A], C, T] {
      type D = A
      def extract(k: C[B[A]]): T[D] = k.asInstanceOf[T[D]]
    }
}

object Test extends LowPriorityImplicit {
implicit def recurEq[
  A <: Tag,
  B[_ <: Tag] <: NestTag[_],
  C[_ <: Tag] <: NestTag[_],
  T[_ <: Tag] <: NestTag[_]]
(implicit ev: Contains[A, B, T]) = new Contains[B[A], C, T] {
  type D = ev.D
  def extract(k: C[B[A]]): T[D] = ev.extract(k.child.asInstanceOf[B[A]])
}

val k =
  div.test.heheh |(
    p.testCls(id:="heh") | (
      div.child | (
        p.whathaha | a()
  )))

type Compound[B <: Tag] = Div[P[B]]
val div_p = jQ(k).has[Compound]
val pele = jQ(k).has[P]
println(div_p)
println(p)
}

}
