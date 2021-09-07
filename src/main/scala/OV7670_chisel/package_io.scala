package OV7670_chisel

import chisel3._

case class mem_control_io(ADDR_WIDTH : Int) extends Bundle {
  val addr : UInt = UInt(ADDR_WIDTH.W)
  val en   : Bool = Bool()
  val we   : Bool = Bool()
}

class para_io extends Bundle {
  val stride      : UInt = UInt(2.W)
  val matrix_size : UInt = UInt(5.W)
  val filter_size : UInt = UInt(3.W)
  val layer_size  : UInt = UInt(3.W)
  val dim_in      : UInt = UInt(7.W)
  val dim_out     : UInt = UInt(7.W)

//  private var _stride      : UInt = 0.U(2.W)
//  private var _matrix_size : UInt = 0.U(5.W)
//  private var _filter_size : UInt = 0.U(3.W)
//  private var _layer_size  : UInt = 0.U(3.W)
//  private var _dim_in      : UInt = 0.U(7.W)
//  private var _dim_out     : UInt = 0.U(7.W)
//
//  def stride : UInt = _stride
//  def stride (new_value : Int) : Unit = {
//    _stride = new_value.asUInt(2.W)
//  }
//
//  def matrix_size : UInt = _matrix_size
//  def matrix_size (new_value : Int) : Unit = {
//    _matrix_size = new_value.asUInt(5.W)
//  }
//
//  def filter_size : UInt = _filter_size
//  def filter_size (new_value : Int) : Unit = {
//    _filter_size = new_value.asUInt(3.W)
//  }
//
//  def layer_size : UInt = _layer_size
//  def layer_size (new_value : Int) : Unit = {
//    _layer_size = new_value.asUInt(3.W)
//  }
//
//  def dim_in : UInt = _dim_in
//  def dim_in (new_value : Int) : Unit = {
//    _dim_in = new_value.asUInt(7.W)
//  }
//
//  def dim_out : UInt = _dim_out
//  def dim_out (new_value : Int) : Unit = {
//    _dim_out = new_value.asUInt(7.W)
//  }
}
