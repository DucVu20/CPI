package CPI.DualClockCPI

import CPI.clockDivider
import chisel3._
import chisel3.util._

class CaptureInterface(bufferDepth: Int) extends Module{
  val io=IO(new Bundle{
    val pclk          = Input (Clock())
    val href          = Input (Bool())
    val vsync         = Input (Bool())
    val pixelIn       = Input (UInt(8.W))
    val pixelOut      = Output(UInt(16.W))  // remember to rename to acquiredPixel
    val frameDone     = Output(Bool())
    val frameWidth    = Output(UInt(log2Ceil(640).W))
    val frameHeight   = Output(UInt(log2Ceil(480).W))
    val imageFormat   = Input(UInt(1.W))
    val capture       = Input(Bool())
    val capturing     = Output(Bool())
    val pixelValid    = Output(Bool())
    val pixelAddress  = Output(UInt(log2Ceil(bufferDepth).W))
    val FMS           = Output(UInt(1.W))

    val pixelIndex  = Output(UInt(1.W))   // peek out for testing


  })

  withClock(io.pclk){
    val idle :: capture_frame :: Nil = Enum(2)
    val FMS=RegInit(idle)
    val firstByte           = RegInit(0.U(8.W))
    val secondByte          = RegInit(0.U(8.W))
    val pixelIndex          = RegInit(0.U((1.W)))
    val rowCnt              = RegInit(0.U(log2Ceil(480).W))
    val colCnt              = RegInit(0.U(log2Ceil(640).W))
    val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
    val frameDone           = RegInit(false.B)
    val pixelValid          = RegInit(false.B)
    val capturing           = WireInit(false.B)

    val vsyncFallingEdge  = (!io.vsync) & (RegNext(io.vsync))

    //====================FMS for capturing images============================//
    switch(FMS) {
      is(idle) {
        when(io.vsync) {
          FMS := idle
        }.otherwise {
          when(io.capture) {
            when(vsyncFallingEdge){
              FMS                 := capture_frame
              frameDone           := false.B
              bufferDepthCounter  := 0.U
            }
          }
        }
        capturing  := false.B
        pixelValid := false.B
      }
      is(capture_frame) {
        capturing  := true.B
        frameDone  := Mux(io.vsync, true.B, false.B)
        FMS        := Mux(io.vsync, idle, capture_frame)

        when(io.href) {
          when(io.imageFormat===0.U){     //gray scale, 8 bit for 1 pixel
            firstByte          := 0.U
            secondByte         := io.pixelIn
            bufferDepthCounter := bufferDepthCounter + 1.U
            colCnt             := colCnt + 1.U
            pixelValid         := true.B
          }otherwise {              // RGB 16 bit modes
            pixelIndex := (!pixelIndex)
            switch(pixelIndex) {
              is(0.U) {
                firstByte  := io.pixelIn
                pixelValid := false.B
              }
              is(1.U) {
                secondByte         := io.pixelIn
                colCnt             := colCnt + 1.U
                bufferDepthCounter := bufferDepthCounter + 1.U
                pixelValid         := true.B
              }
            }
          }
        }.otherwise{
          pixelValid := false.B
        }
      }
    }
    //====================resolution counter===============//
    val hrefRisingEdge    = io.href & (!RegNext(io.href))

    when(hrefRisingEdge) {
      rowCnt := rowCnt + 1.U
      colCnt := 0.U
    }

    when(vsyncFallingEdge) {
      rowCnt := 0.U
      colCnt := 0.U
    }
    //=========================output wire connection=========================//
    io.pixelOut     := Cat(firstByte, secondByte)
    io.frameDone    := frameDone
    io.frameWidth   := colCnt
    io.frameHeight  := rowCnt
    io.capturing    := capturing
    io.pixelValid   := pixelValid
    io.pixelAddress := RegNext(bufferDepthCounter)
    io.FMS          := FMS

    io.pixelIndex  := pixelIndex // peek out for testing

  }
}

//=======================DEMO CAPTURE INTERFACE DUAL CLOCK==================//
class CaptureInterfaceDemo(bufferDepth: Int,
                           max_prescaler: Int ) extends Module {
  val io = IO(new Bundle {
    val pclk        = Input(Clock())
    val href        = Input(Bool())
    val vsync       = Input(Bool())
    val pixelIn     = Input(UInt(8.W))
    val pixelOut    = Output(UInt(16.W)) // remember to change to acquiredPixel
    val frameDone   = Output(Bool())
    val frameWidth  = Output(UInt(log2Ceil(640).W))
    val frameHeight = Output(UInt(log2Ceil(480).W))
    val imageFormat = Input(UInt(1.W))
    val capture     = Input(Bool())
    val capturing   = Output(Bool())
    val pixelValid  = Output(Bool())
    val pixelAddress  = Output(UInt(log2Ceil(bufferDepth).W))
    val prescaler   = Input(UInt(log2Ceil(max_prescaler).W))

    val pixelIndex= Output(UInt(1.W))

  })

  val clock_div = Module(new clockDivider(max_prescaler))
  val capture_interface = Module(new CaptureInterface(bufferDepth))

  clock_div.io.clock_in  := (!clock.asBool).asClock()
  clock_div.io.reset     := reset
  clock_div.io.prescaler := io.prescaler

  capture_interface.io.pclk        <> clock_div.io.divided_clock
  capture_interface.io.href        <> io.href
  capture_interface.io.vsync       <> io.vsync
  capture_interface.io.pixelIn     <> io.pixelIn
  capture_interface.io.pixelOut    <> io.pixelOut
  capture_interface.io.frameDone   <> io.frameDone
  capture_interface.io.frameWidth  <> io.frameWidth
  capture_interface.io.frameHeight <> io.frameHeight
  capture_interface.io.imageFormat <> io.imageFormat
  capture_interface.io.capture     <> io.capture
  capture_interface.io.capturing   <> io.capturing
  capture_interface.io.pixelValid  <> io.pixelValid
  capture_interface.io.pixelAddress <> io.pixelAddress

  io.pixelIndex := capture_interface.io.pixelIndex // peek out for testing

}