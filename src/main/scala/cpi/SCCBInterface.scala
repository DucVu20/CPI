package sislab.cpi

import chisel3._
import chisel3.util._

class SCCBInterface(CLK_FREQ_MHz: Int, SCCB_FREQ_KHz: Int) extends Module{
  val MHz=scala.math.pow(10,6)
  val kHz=scala.math.pow(10,3)
  val timer=(CLK_FREQ_MHz*MHz/(SCCB_FREQ_KHz*kHz)).toInt

  val io=IO(new Bundle{
    val config         = Input(Bool())
    val sccbReady      = Output(Bool())
    val SIOC           = Output(Bool())
    val SIOD           = Output(Bool())
    val configData     = Input(UInt(8.W))
    val controlAddress = Input(UInt(8.W))
  })
  // as we only need to set up working modes for the camera, 3 phase write transmission cycle is employed
  // phase 1: ID Address, which is "h42" for OV7670
  // phase 2: sub-address, control registers inside the camera
  // phase 3: data to be overwritten to control registers inside OV7670 to specify working mode

  val OV7670WriteAddr = "h42".U
  val sccbReady      = RegInit(true.B)

  val latchedAddr    = RegInit(0.U(8.W))
  val latchedData    = RegInit(0.U(8.W))

  val sccbTimer      = RegInit(0.U(32.W))
  val txByte         = RegInit(0.U(8.W))
  val byteIndex      = RegInit(0.U(4.W))
  val byteCounter    = RegInit(0.U(2.W))
  val SIOC           = RegInit(false.B)
  val SIOD           = RegInit(false.B)
  val FmsReturnState = RegInit(0.U(4.W))

  val fmsIdle::fmsStart::fmsLoadByte::fmsTxByte1::fmsTxByte2::fmsTxByte3::fmsTxByte4::fmsEndSignal1::fmsEndSignal2::fmsEndSignal3::fmsEndSignal4::fmsDone::fmsTimer::Nil = Enum(13)
  val FMS               = RegInit(fmsIdle)

  io.sccbReady  := sccbReady
  io.SIOD       := SIOD
  io.SIOC       := SIOC

  switch(FMS){
    is(fmsIdle){
      SIOC := 0.U     // bring SIOC, SIOD high
      SIOD := 0.U
      sccbReady := true.B
      when(io.config){
        FMS       := fmsStart
        sccbReady := false.B
        latchedData := io.configData
        latchedAddr := io.controlAddress
      }
      byteIndex   := 0.U
      byteCounter := 0.U
    }
    is(fmsStart){           // bring SIOC high, SIOD low
      SIOC            := 0.U
      SIOD            := 1.U
      FMS             := fmsTimer            // generate SIOC
      FmsReturnState  := fmsLoadByte
      sccbTimer       := (timer/4).U
    }
    is(fmsLoadByte){    // 1 clock
      FMS       := Mux(byteCounter===3.U, fmsEndSignal1, fmsTxByte1)
      byteIndex   := 0.U
      byteCounter := byteCounter + 1.U
      switch(byteCounter){
        is(0.U) { txByte := OV7670WriteAddr}
        is(1.U) { txByte := latchedAddr}
        is(2.U) { txByte := latchedData}
        is(3.U) { txByte := latchedData}
      }
    }
    is(fmsTxByte1){  // SIOC low and delay for next state
      FMS            := fmsTimer
      FmsReturnState := fmsTxByte2
      sccbTimer      := (timer/4).U
      SIOC           := 1.U
    }
    is(fmsTxByte2){      // assign output data
      FMS            := fmsTimer
      FmsReturnState := fmsTxByte3
      sccbTimer      := (timer/4).U
      SIOD           := Mux(byteIndex === 8.U, 0.U, ~txByte(7))
    }
    is(fmsTxByte3){   // bring SIOC high
      FMS             := fmsTimer
      FmsReturnState  := fmsTxByte4
      sccbTimer       := (timer/2).U
      SIOC            := 0.U // output enable is an inverting pulldown
    }
    is(fmsTxByte4){  //check for end of byte, increment counter
      FMS            := Mux(byteIndex===8.U, fmsLoadByte, fmsTxByte1)
      txByte         := txByte<<1    // shift in next data bit
      byteIndex      := byteIndex + 1.U
    }
    is(fmsEndSignal1){  // state s entered with SIOC high, SIOD high. Start bringing SIOC low
      FMS            := fmsTimer
      FmsReturnState := fmsEndSignal2
      sccbTimer      := (timer/4).U
      SIOC           := 1.U
    }
    is(fmsEndSignal2){ // while SIOC is low, bring SIOD low
      FMS            := fmsTimer
      FmsReturnState := fmsEndSignal3
      sccbTimer      := (timer/4).U
      SIOD           := 1.U
    }
    is(fmsEndSignal3){  // bring SIOC high
      FMS             := fmsTimer
      FmsReturnState  := fmsEndSignal4
      sccbTimer       := (timer/4).U
      SIOC            := 0.U
    }
    is(fmsEndSignal4){  // bring SIOD high when SIOC is high
      FMS            := fmsTimer
      FmsReturnState := fmsDone
      sccbTimer      := (timer/4).U
      SIOC           := 0.U
    }
    is(fmsDone){      // add delay between transactions
      FMS            := fmsTimer
      FmsReturnState := fmsIdle
      sccbTimer      := (2*timer).U
      byteCounter    := 0.U
    }
    is(fmsTimer){
      FMS       := Mux(sccbTimer===0.U, FmsReturnState, fmsTimer)
      sccbTimer := Mux(sccbTimer===0.U, 0.U, sccbTimer - 1.U)
    }
  }
}
