import scala.math._

object PARA {
  val NO_TEST        = 1
  val BIT_WIDTH_W    = 12
  val BIT_WIDTH_A    = 8
  val NUM_CONCURRENT = 3
  val NUM_BIT_IN     : Int = ceil(log(BIT_WIDTH_W)/log(2)).toInt
  val NUM_EXT_BIT    : Int = ceil(log(NUM_CONCURRENT)/log(2)).toInt

  def in_range(x : Int, n : Int) : Int = {
    if (x < 0) {
      (abs(x) % pow(2, n-1).toInt) * (-1)
    } else {
      x % pow(2, n-1).toInt
    }
  }

  def in_range_pos(x : Int, n : Int) : Int = abs(x) % pow(2, n).toInt
  //=============== Convolution PARA ==================== //
  val MATRIX_WIDTH  = 32
  val MATRIX_HEIGHT = 32
  val FILTER_WIDTH  = 5
  val FILTER_HEIGHT = 5
  val STRIDE        = 2
  val FBIT_WIDTH    = 9
  val DBIT_WIDTH    = 9

  val DIR_DATA_FILE = "../golden_para/"
}