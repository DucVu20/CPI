package CPI
import chisel3._
import chisel3.util._

class captureInterface(bufferDepth: Int) extends Module{
  val io=IO(new Bundle{
    val pclk         = Input (Clock())
    val href          = Input (Bool())
    val vsync         = Input (Bool())
    val pixelIn       = Input (UInt(8.W))
    val pixelOut      = Output(UInt(16.W))  // remember to change to acquiredPixel
    val frameDone     = Output(Bool())
    val frameWidth    = Output(UInt(log2Ceil(640).W))
    val frameHeight   = Output(UInt(log2Ceil(480).W))
    val imageFormat   = Input(UInt(1.W))
    val capture       = Input(Bool())
    val capturing     = Output(Bool())
  })

  withClock(io.pclk){
    val idle :: capture_frame :: Nil = Enum(2)
    val FMS=RegInit(idle)
    val firstByte           = RegInit(0.U(8.W))
    val secondByte          = RegInit(0.U(8.W))
    val pixel               = Cat(firstByte,secondByte)
    val pixelIndex          = RegInit(0.U((1.W)))
    val rowCnt              = RegInit(0.U(log2Ceil(480).W))
    val colCnt              = RegInit(0.U(log2Ceil(640).W))
    val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
    val frameDone           = RegInit(false.B)
    val pixelValid          = RegInit(false.B)
    val captureSignalHolder = RegInit(false.B)
    val capturing           = WireInit(false.B)

    captureSignalHolder    := Mux(io.capture, io.capture, captureSignalHolder)

    //====================FMS for capturing images============================//

    switch(FMS) {
      is(idle) {
        when(io.vsync) {
          FMS := idle
        }.otherwise {
          when(captureSignalHolder) {
            FMS                 := capture_frame
            captureSignalHolder := false.B
            frameDone           := false.B
          }
        }
        capturing := false.B
      }
      is(capture_frame) {
        capturing  := true.B
        frameDone  := Mux(io.vsync, true.B, false.B)
        FMS        := Mux(io.vsync, idle, capture_frame)

        when(io.href) {
          when(io.imageFormat===0.U){     //gray scale, 8 bit for 1 pixel
            firstByte  := 0.U
            secondByte  := io.pixelIn
            colCnt      := colCnt + 1.U
            bufferDepthCounter := bufferDepthCounter + 1.U

          }otherwise {              // RGB 16 bit modes
            pixelIndex := (!pixelIndex)
            switch(pixelIndex) {
              is(0.U) {
                firstByte  := io.pixelIn
              }
              is(1.U) {
                secondByte         := io.pixelIn
                colCnt             := colCnt + 1.U
                bufferDepthCounter := bufferDepthCounter + 1.U
              }
            }
          }
        }
      }
    }
    //====================resolution counter===============//
    val hrefRisingEdge    = io.href & (!RegNext(io.href))
    val vsyncFallingEdge  = RegNext(io.vsync) & (!io.vsync)

    when(hrefRisingEdge) {
      rowCnt := rowCnt + 1.U
      colCnt := 0.U
    }
    when(vsyncFallingEdge) {
      rowCnt := 0.U
      colCnt := 0.U
    }
    //=========================output wire connection=========================//
    io.pixelOut    := pixel
    io.frameDone   := frameDone
    io.frameWidth  := colCnt
    io.frameHeight := rowCnt
    io.capturing   := capturing
  }

}
class captureModuleDualClock(imgWidth: Int, imgHeight: Int) extends Module{

  val w = imgWidth
  val h = imgHeight
  val bufferDepth = imgWidth * imgHeight // the maximum depth of the buffer
  val pixelBits = 16

  val io  = IO(new Bundle {
    val pclk         = Input (Clock())
    val href          = Input (Bool())
    val vsync         = Input (Bool())
    val pixelIn       = Input (UInt(8.W))

    val pixelOut      = Output(UInt(pixelBits.W))
    val pixelAddr     = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth    = Output(UInt(log2Ceil(640).W))
    val frameHeight   = Output(UInt(log2Ceil(480).W))
    val imageFormat   = Input(UInt(1.W))
    val capture       = Input (Bool())
    val capturing     = Output(Bool())
    val read_frame    = Input (Bool()) // ready
    val bufferStatus  = Output(Bool()) // valid
    val frameDone     = Output(Bool())
  })

  val dualClockBuffer    = Module(new DualClockRam(bufferDepth,UInt(pixelBits.W)))
  val captureInterface = Module(new captureInterface(bufferDepth))

  val bufferFull          = RegInit(false.B)
  val readPtr             = RegInit(0.U(log2Ceil(bufferDepth).W))
  val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))

  val wrEnaWire  = WireInit(false.B)

  //=====================READ ADDRESS GENERATOR==================//
  when(io.read_frame) {
    readPtr    := readPtr + 1.U
    when(readPtr === (bufferDepthCounter - 1.U)) {
      readPtr            := 0.U
      bufferDepthCounter := 0.U
    }
  }


  //==============================IO=========================================//
  captureInterface.io.pclk := io.pclk
  captureInterface.io.pixelIn :=io.pixelIn
  captureInterface.io.href  := io.href
  captureInterface.io.vsync := io.vsync
  captureInterface.io.capture :=io.capture
  captureInterface.io.imageFormat := io.imageFormat

  io.capturing  := captureInterface.io.capturing
  io.frameWidth := captureInterface.io.frameWidth
  io.frameHeight := captureInterface.io.frameHeight
  io.frameDone   := captureInterface.io.frameDone
  io.pixelAddr  := readPtr      // optional, generate to ease verifications
  io.bufferStatus := bufferFull

  dualClockBuffer.clock := clock
  dualClockBuffer.io.writeClock := io.pclk
  dualClockBuffer.io.dataIn     := captureInterface.io.pixelOut
  dualClockBuffer.io.readAddr   := readPtr
  dualClockBuffer.io.dataOut   := io.pixelOut

}
