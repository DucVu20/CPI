package sislab.cpi

import chisel3._
import chisel3.util._

class SCCBInterface(CLK_FREQ_MHz: Int, SCCB_FREQ_KHz: Int) extends Module{
  val MHz=scala.math.pow(10,6)
  val kHz=scala.math.pow(10,3)
  val timer=(CLK_FREQ_MHz*MHz/(SCCB_FREQ_KHz*kHz*2)).toInt

  val io=IO(new Bundle{
    val config         = Input(Bool())
    val sccbReady      = Output(Bool())
    val SIOC           = Output(Bool())
    val SIOD           = Output(UInt(1.W))
    val configData     = Input(UInt(8.W))
    val controlAddress = Input(UInt(8.W))
  })
  // as we only need to set up working modes for the camera, 3 phase write transmission cycle is employed
  // phase 1: ID Address, which is "h42" for OV7670
  // phase 2: sub-address, control registers inside the camera
  // phase 3: data to be overwritten to control registers inside OV7670 to specify working mode

  val OV7670WriteAddr = "h42".U
  val sccbReady       = WireInit(true.B)

  val latchedAddr    = RegInit(0.U(8.W))
  val latchedData    = RegInit(0.U(8.W))

  val sccbTimer      = RegInit(0.U(32.W))
  val txByte         = RegInit(0.U(8.W))
  val byteIndex      = RegInit(0.U(4.W))
  val byteCounter    = RegInit(0.U(2.W))
  val SIOC           = RegInit(false.B)
  val SIOD           = RegInit(false.B)
  val FmsReturnState = RegInit(0.U(4.W))

  val fms_idle::fms_start::fms_load_byte::fms_tx_byte_low::fms_tx_byte_high::fms_index_check::fms_stop1::fms_stop2::fms_done::fms_sccb_timer::Nil = Enum(10)
  val FMS               = RegInit(fms_idle)

  io.sccbReady  := sccbReady
  io.SIOD       := SIOD
  io.SIOC       := SIOC

  switch(FMS){
    is(fms_idle){
      SIOC := false.B     // bring SIOC, SIOD high
      SIOD := false.B
      sccbReady := true.B
      when(io.config){
        FMS        := fms_start
        sccbReady := false.B
      }
      latchedData := io.configData
      latchedAddr := io.controlAddress
      txByte      := latchedData
      byteIndex   := 0.U
      byteCounter := 0.U
    }
    is(fms_start){           // bring SIOC high, SIOD low
      sccbReady       := false.B
      SIOC            := false.B
      SIOD            := true.B
      FMS             := fms_sccb_timer            // generate SIOC
      FmsReturnState  := fms_load_byte
      sccbTimer       := timer.U
    }
    is(fms_load_byte){    // 1 clock
      sccbReady := false.B
      FMS        := fms_tx_byte_low
      when(byteCounter===3.U ){
        FMS:=fms_stop1
      }
      byteIndex   := 0.U
      byteCounter := byteCounter + 1.U
      switch(byteCounter){
        is(0.U) { txByte := OV7670WriteAddr}
        is(1.U) { txByte := latchedAddr}
        is(2.U) { txByte := latchedData}
        is(3.U) { txByte := latchedData}
      }
    }
    is(fms_tx_byte_low){            // start inserting SIOD when SIOC is low for t_sccb/2
      sccbReady      := false.B
      SIOC           := true.B
      FMS            := fms_sccb_timer     // data on low edge of SIOC
      FmsReturnState := fms_tx_byte_high
      sccbTimer      := timer.U
      SIOD           := Mux(byteIndex===8.U,0.U,!txByte(7))
    }
    is(fms_tx_byte_high){       // keep SIOD same as when SIOC is high, finish one sccb clock for SIOD
      sccbReady      := false.B
      SIOC           := false.B
      FMS            := fms_sccb_timer
      sccbTimer      := (timer/2).toInt.U
      FmsReturnState := fms_index_check
    }
    is(fms_index_check){
      sccbReady       := false.B
      byteIndex := byteIndex + 1.U
      txByte    := txByte<<1
      when(byteIndex===8.U){           // when one byte is loaded successfully, along with dont care bit, load the next byte
        FMS := fms_load_byte
      }.otherwise{
        FMS := fms_tx_byte_low
      }
    }
    is(fms_stop1){  // bring SIOC high, SIOD low
      sccbReady      := false.B
      SIOC           := false.B
      SIOD           := true.B
      sccbTimer      := timer.U
      FMS            := fms_sccb_timer
      FmsReturnState := fms_stop2
    }
    is(fms_stop2){  // bring SIOD high
      sccbReady      := false.B
      SIOD           := false.B
      SIOC           := false.B
      sccbTimer      := timer.U
      FMS            := fms_sccb_timer
      FmsReturnState := fms_done
    }
    is(fms_done){ // add delay between transactions
      sccbReady      := false.B
      sccbTimer      := timer.U
      FmsReturnState := fms_idle
      FMS            := fms_sccb_timer
    }
    is(fms_sccb_timer){           // delay state to create SIOC low and high
      sccbReady       := false.B
      FMS := Mux(sccbTimer===0.U, FmsReturnState, FMS)
      sccbTimer := Mux(sccbTimer===0.U,0.U, sccbTimer - 1.U)
    }
  }
}