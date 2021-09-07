package CPI

import CPI.formats._
import chisel3._
import chisel3.util._

/**
  this module converts various RGB formats, namely RGB565,RGB555, RGB444 to RGB888.
  Hardware costs are reduced by reusing a multiplexer and a barrel shifter.
 Therefore, this architecture takes 3 clock cycle to convert 1 pixel in 2 byte format
 to RGB888
 - Version 1 is the one clock cycle converter
 - Version 2 is the 3 clock cycle converter
*/

object formats {
  val RGB565 = 0
  val RGB555 = 1
  val RGB444 = 2
}

class pixel_converter extends Module{
  val io=IO(new Bundle{
    val pixel_in=Input(UInt(16.W))
    val pixel_out=Output(UInt(24.W))
    val pixel_in_valid=Input(Bool())
    val pixel_out_valid=Output(Bool())
    val input_format=Input(UInt(2.W))
  })
  val red=RegInit(0.U(8.W))
  val green=RegInit(0.U(8.W))
  val blue=RegInit(0.U(8.W))
  val RGB888=Cat(red,green,blue)
  io.pixel_out:=RGB888
  io.pixel_out_valid:=false.B

  when(io.pixel_in_valid){
    when(io.input_format===RGB565.asUInt){
      red:=(io.pixel_in(15,11)*526.U)>>6
      green:=(io.pixel_in(10,5)*259.U)>>6
      blue:=(io.pixel_in(4,0)*526.U)>>6
      io.pixel_out_valid:=true.B
    }.elsewhen(io.input_format===RGB555.asUInt){
      red:=(io.pixel_in(14,10)*526.U)>>6
      green:=(io.pixel_in(9,5)*526.U)>>6
      blue:=(io.pixel_in(4,0)*526.U)>>6
      io.pixel_out_valid:=true.B
    }.elsewhen(io.input_format===RGB444.asUInt){
      red:=io.pixel_in(11,8)*17.U
      green:=io.pixel_in(7,4)*17.U
      blue:=io.pixel_in(3,0)*17.U
      io.pixel_out_valid:=true.B
    }.otherwise{
      red:=0.U
      green:=0.U
      blue:=0.U
      io.pixel_out_valid:=false.B
    }
  }
}


class pixel_converter_shift_ver1 extends Module{
  val io=IO(new Bundle{
    val pixel_in=Input(UInt(16.W))
    val pixel_out=Output(UInt(24.W))
    val pixel_in_valid=Input(Bool())
    val pixel_out_valid=Output(Bool())
    val input_format=Input(UInt(2.W))
  })
  val red=RegInit(0.U(8.W))
  val green=RegInit(0.U(8.W))
  val blue=RegInit(0.U(8.W))
  val RGB888=Cat(red,green,blue)
  io.pixel_out:=RGB888
  io.pixel_out_valid:=false.B

  // Instantiate components to perform data conversion
  when(io.pixel_in_valid){
    when(io.input_format===RGB565.asUInt){
      red:=io.pixel_in(15,11)<<3
      green:=io.pixel_in(10,5)<<2
      blue:=io.pixel_in(4,0)<<3
      io.pixel_out_valid:=true.B
    }.elsewhen(io.input_format===RGB555.asUInt){
      red:=io.pixel_in(14,10)<<3
      green:=io.pixel_in(9,5)<<3
      blue:=io.pixel_in(4,0)<<3
      io.pixel_out_valid:=true.B
    }.elsewhen(io.input_format===RGB444.asUInt){
      red:=io.pixel_in(11,8)<<4
      green:=io.pixel_in(7,4)<<4
      blue:=io.pixel_in(3,0)<<4
      io.pixel_out_valid:=true.B
    }.otherwise{
      red:=0.U
      green:=0.U
      blue:=0.U
      io.pixel_out_valid:=false.B
    }
  }
}
