package sislab.cpi

import chisel3._
import chisel3.util._

class CaptureModule(imgWidthCnt: Int, imgHeightCnt: Int,
                    bytePerPixel: Int,
                    bufferDepth: Int)  extends Module {

  val pixelBits = 8*bytePerPixel

  val io  = IO(new Bundle {
    val pclk         = Input (Bool())
    val href         = Input (Bool())
    val vsync        = Input (Bool())
    val pixelIn      = Input (UInt(8.W))
    val pixelOut     = Output(UInt(pixelBits.W))
    val pixelAddr    = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth   = Output(UInt(log2Ceil(imgWidthCnt).W))
    val frameHeight  = Output(UInt(log2Ceil(imgHeightCnt).W))
    val grayImage    = Input(Bool())                // 0 for RGB 1 for gray
    val capture      = Input (Bool())
    val capturing    = Output(Bool())
    val readFrame    = Input (Bool()) // ready
    val frameFull    = Output(Bool())// valid
  })

  val idle :: captureFrame :: Nil = Enum(2)
  val FMS = RegInit(idle)

  val writePtr            = RegInit(0.U(log2Ceil(bufferDepth).W))
  val readPtr             = RegInit(0.U(log2Ceil(bufferDepth).W))
  val firstByte           = RegInit(0.U(8.W))
  val secondByte          = RegInit(0.U(8.W))
  val pixel               = Cat(firstByte,secondByte)
  val captureSignalHolder = RegInit(false.B)  // used to hold the capture
  // signal to capture an image when signals for a new frame is available
  val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
  val frameFull           = RegInit(false.B)
  val pixelIndex          = RegInit(0.U(log2Ceil(bytePerPixel).W))
  val rowCnt              = RegInit(0.U(log2Ceil(imgHeightCnt).W))
  val colCnt              = RegInit(0.U(log2Ceil(imgWidthCnt).W))
  val bufferEmpty         = RegInit(true.B)

  val bufferAddr = WireInit(0.U(log2Ceil(bufferDepth).W))
  val capturing  = WireInit(false.B)
  val wrEnaWire  = WireInit(false.B)
  val wrEna      = RegNext(wrEnaWire)

  val buffer = Module(new SinglePortRam(bufferDepth,UInt(pixelBits.W)))

  val pclkRisingEdge   = io.pclk & (!RegNext(io.pclk))
  val vsyncFallingEdge = (!io.vsync) & RegNext(io.vsync)
  val hrefFallingEdge  = (!io.href) & RegNext(io.href)
  val hrefRisingEdge   = io.href & (!RegNext(io.href))

  //=====================READ ADDRESS GENERATOR==================//
  when(io.readFrame & (!bufferEmpty)) {
    bufferAddr := readPtr
    readPtr    := readPtr + 1.U
    when(readPtr === (bufferDepthCounter - 1.U)) {
      readPtr            := 0.U
      bufferDepthCounter := 0.U
      frameFull          := false.B
      bufferEmpty        := true.B
    }
  } otherwise {
    bufferAddr := RegNext(writePtr)
  }

  captureSignalHolder  := Mux(io.capture, io.capture, captureSignalHolder)

  //====================FMS for capturing images============================//
  switch(FMS) {
    is(idle) {
      when(io.vsync) {
        FMS := idle
      } otherwise {
        when(captureSignalHolder) {
          when(vsyncFallingEdge) {
            FMS                 := captureFrame
            captureSignalHolder := false.B
            frameFull           := false.B
            rowCnt              := 0.U
            colCnt              := 0.U   // reset counters
          }
        }
      }
      writePtr  := 0.U
      capturing := false.B
    }
    is(captureFrame) {

      bufferEmpty := false.B
      capturing := true.B
      frameFull := Mux(io.vsync, true.B, false.B)
      FMS       := Mux(io.vsync, idle, captureFrame)

      when(pclkRisingEdge && io.href) {
        when(io.grayImage){     //gray scale, 8 bit for 1 pixel
          firstByte  := 0.U
          secondByte := io.pixelIn
          wrEnaWire  := pclkRisingEdge
          writePtr   := writePtr + 1.U
          colCnt     := colCnt + 1.U
        }otherwise {                   // RGB 16 bit modes
          pixelIndex := (!pixelIndex)
          switch(pixelIndex) {
            is(0.U) {
              firstByte  := io.pixelIn
            }
            is(1.U) {
              secondByte   := io.pixelIn
              wrEnaWire    := pclkRisingEdge
              writePtr     := writePtr + 1.U
              colCnt       := colCnt + 1.U
            }
          }
        }
      }
      when(hrefRisingEdge){
        rowCnt := rowCnt + 1.U
        colCnt := 0.U
      }
      when(hrefFallingEdge) {
        bufferDepthCounter := writePtr
      }
    }
  }
  //=======================connect signals to the buffer========================//
  buffer.io.wrEna   := wrEna
  buffer.io.rdEna   := io.readFrame
  buffer.io.addr    := bufferAddr
  buffer.io.dataIn  := pixel

  //==============================IO============================================//
  io.capturing    := capturing
  io.frameWidth   := colCnt
  io.frameHeight  := rowCnt
  io.pixelAddr    := RegNext(readPtr)
  io.pixelOut     := buffer.io.dataOut
  io.frameFull    := frameFull

}