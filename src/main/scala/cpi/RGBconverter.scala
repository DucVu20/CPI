package sislab.cpi

import chisel3._
import chisel3.util._

/**
  this module converts various RGB formats, namely RGB565,RGB555, RGB444 to RGB888.
  Hardware costs are reduced by reusing a multiplexer and a barrel shifter.
 Therefore, this architecture takes 3 clock cycle to convert 1 pixel in 2 byte format
 to RGB888
*/

object format {
  val RGB565 = 0
  val RGB555 = 1
  val RGB444 = 2
}

class pixel_converter extends Module{
  val io=IO(new Bundle{
    val pixelIn       = Input(UInt(16.W))
    val pixelOut      = Output(UInt(24.W))
    val pixelIn_valid = Input(Bool())
    val pixelOutValid = Output(Bool())
    val inputFormat   = Input(UInt(2.W))
  })
  val red    = RegInit(0.U(8.W))
  val green  = RegInit(0.U(8.W))
  val blue   = RegInit(0.U(8.W))
  val RGB888 = Cat(red,green,blue)

  io.pixelOut      := RGB888
  io.pixelOutValid := false.B

  when(io.pixelIn_valid){
    when(io.inputFormat === format.RGB565.asUInt){
      red              := (io.pixelIn(15,11)*526.U) >>6
      green            := (io.pixelIn(10,5)*259.U)  >>6
      blue             := (io.pixelIn(4,0)*526.U)   >>6
      io.pixelOutValid := true.B
    }.elsewhen(io.inputFormat === format.RGB555.asUInt){
      red              := (io.pixelIn(14,10)*526.U)>>6
      green            := (io.pixelIn(9,5)*526.U)>>6
      blue             := (io.pixelIn(4,0)*526.U)>>6
      io.pixelOutValid := true.B
    }.elsewhen(io.inputFormat===format.RGB444.asUInt){
      red              := io.pixelIn(11,8)*17.U
      green            := io.pixelIn(7,4)*17.U
      blue             := io.pixelIn(3,0)*17.U
      io.pixelOutValid := true.B
    }.otherwise{
      red              := 0.U
      green            := 0.U
      blue             := 0.U
      io.pixelOutValid := false.B
    }
  }
}

class pixelConverterShiftVer extends Module{
  val io=IO(new Bundle{
    val pixelIn       = Input(UInt(16.W))
    val pixelOut      = Output(UInt(24.W))
    val pixelInValid  = Input(Bool())
    val pixelOutValid = Output(Bool())
    val inputFormat   = Input(UInt(2.W))
  })
  val red    = RegInit(0.U(8.W))
  val green  = RegInit(0.U(8.W))
  val blue   = RegInit(0.U(8.W))
  val RGB888 = Cat(red,green,blue)
  io.pixelOut      := RGB888
  io.pixelOutValid := false.B

  // Instantiate components to perform data conversion
  when(io.pixelInValid){
    when(io.inputFormat===format.RGB565.asUInt){
      red              := io.pixelIn(15,11)<<3
      green            := io.pixelIn(10,5)<<2
      blue             := io.pixelIn(4,0)<<3
      io.pixelOutValid := true.B
    }.elsewhen(io.inputFormat===format.RGB555.asUInt){
      red              := io.pixelIn(14,10)<<3
      green            := io.pixelIn(9,5)<<3
      blue             := io.pixelIn(4,0)<<3
      io.pixelOutValid := true.B
    }.elsewhen(io.inputFormat === format.RGB444.asUInt){
      red              := io.pixelIn(11,8)<<4
      green            := io.pixelIn(7,4)<<4
      blue             := io.pixelIn(3,0)<<4
      io.pixelOutValid := true.B
    }.otherwise{
      red              := 0.U
      green            := 0.U
      blue             := 0.U
      io.pixelOutValid := false.B
    }
  }
}
