case class Dog(name:String, dogs: Seq[Dog])

class DogIterator(dogs: Seq[Dog]) extends Iterator[Dog]{
  var nextDog = 0
  override def hasNext: Boolean = nextDog < dogs.size

  override def next(): Dog = {
    ???
  }
}