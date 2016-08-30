
import org.scalatest._
import com.ligadata.runtime.Validation._

class TestValidation extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  def HandleError(fieldName : String, checkName : String) : Unit = {
    val err = fieldName + " - check (" + checkName + ") violated "
    println(err)
  }

  describe("test isNull"){
    it(""){
      val check1 = !isNull("", "name", HandleError)
      val check2 = !isNull(null, "name", HandleError)
      val check3 = !isNull("123", "id", HandleError)
      check1 shouldEqual false
      check2 shouldEqual false
      check3 shouldEqual true

    }
  }

}
