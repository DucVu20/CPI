package sislab.cpi

import chisel3._
import chisel3.util._


class CaptureModule(imgWidth: Int, imgHeight: Int,
                    bytePerPixel: Int,
                    bufferDepth: Int )  extends Module {
  val w = imgWidth
  val h = imgHeight
  val pixelBits = 8*bytePerPixel

  val io  = IO(new Bundle {
    val pclk         = Input (Bool())
    val href         = Input (Bool())
    val vsync        = Input (Bool())
    val pixelIn      = Input (UInt(8.W))
    val pixelOut     = Output(UInt(pixelBits.W))
    val pixelAddr    = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth   = Output(UInt(log2Ceil(imgWidth).W))
    val frameHeight  = Output(UInt(log2Ceil(imgHeight).W))
    val imageFormat  = Input(UInt(1.W))                // either RGB or gray
    val capture      = Input (Bool())
    val capturing    = Output(Bool())
    val readFrame    = Input (Bool()) // ready
    val frameFull    = Output(Bool()) // valid

  })
  val idle :: capture_frame :: Nil = Enum(2)
  val FMS = RegInit(idle)

  val writePtr            = RegInit(0.U(log2Ceil(bufferDepth).W))
  val readPtr             = RegInit(0.U(log2Ceil(bufferDepth).W))
  val firstByte           = RegInit(0.U(8.W))
  val secondByte          = RegInit(0.U(8.W))
  val pixel               = Cat(firstByte,secondByte)
  val captureSignalHolder = RegInit(false.B)  // used to hold the capture
  // signal to capture an image when signals for a new frame is available
  val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
  val frameDone           = RegInit(false.B)
  val pixelIndex          = RegInit(0.U(log2Ceil(bytePerPixel).W))
  val rowCnt              = RegInit(0.U(log2Ceil(imgWidth).W))
  val colCnt              = RegInit(0.U(log2Ceil(imgHeight).W))

  val bufferAddr = WireInit(0.U(log2Ceil(bufferDepth).W))
  val capturing  = WireInit(false.B)
  val wrEnaWire  = WireInit(false.B)
  val wrEna      = RegNext(wrEnaWire)

  val buffer = Module(new single_port_ram(bufferDepth,UInt(pixelBits.W)))

  val pclkRisingEdge   = (io.pclk) & (!RegNext(io.pclk))
  val vsyncFallingEdge = (!io.vsync) & (RegNext(io.vsync))

  //=====================READ ADDRESS GENERATOR==================//
  when(io.readFrame) {
    bufferAddr := readPtr
    readPtr    := readPtr + 1.U
    when(readPtr === (bufferDepthCounter - 1.U)) {
      readPtr            := 0.U
      bufferDepthCounter := 0.U
      frameDone          := false.B
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
            FMS                 := capture_frame
            captureSignalHolder := false.B
            frameDone           := false.B
          }
        }
      }
      writePtr  := 0.U
      capturing := false.B
    }
    is(capture_frame) {
      capturing := true.B
      frameDone := Mux(io.vsync, true.B, false.B)
      FMS       := Mux(io.vsync, idle, capture_frame)

      when(pclkRisingEdge && (io.href)) {
        when(io.imageFormat===0.U){     //gray scale, 8 bit for 1 pixel
          firstByte  := 0.U
          secondByte := io.pixelIn
          wrEnaWire  := pclkRisingEdge
          writePtr   := writePtr+1.U
          colCnt     := colCnt + 1.U
          bufferDepthCounter := bufferDepthCounter + 1.U

        }otherwise {                   // RGB 16 bit modes
          pixelIndex := (!pixelIndex)
          switch(pixelIndex) {
            is(0.U) {
              firstByte  := io.pixelIn
            }
            is(1.U) {
              secondByte   := io.pixelIn
              wrEnaWire    := pclkRisingEdge
              writePtr     := writePtr+1.U
              colCnt       := colCnt + 1.U
              bufferDepthCounter := bufferDepthCounter + 1.U
            }
          }
        }
      }
    }
  }
  //=======================connect signals to the buffer========================//
  buffer.io.wrEna   := wrEna
  buffer.io.rdEna   := io.readFrame
  buffer.io.addr    := bufferAddr
  buffer.io.data_in := pixel

  //==============================IO============================================//
  io.capturing    := capturing
  io.frameWidth   := colCnt
  io.frameHeight  := rowCnt
  io.pixelAddr    := RegNext(readPtr)
  io.pixelOut     := buffer.io.data_out
  io.frameFull    := frameDone

  //================GET IMAGE RESOLUTION BASED ON HREF, VSYNC ==================//
  when((io.href) & (!RegNext(io.href))) {
    rowCnt := rowCnt + 1.U
    colCnt := 0.U
  }
  when(vsyncFallingEdge) {
    rowCnt := 0.U
    colCnt := 0.U
  }
}
