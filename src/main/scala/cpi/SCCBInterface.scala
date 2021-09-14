package sislab.cpi

import chisel3._
import chisel3.util._

class SCCBInterface(CLK_FREQ_MHz: Int, SCCB_FREQ_KHz: Int) extends Module{
  val MHz=scala.math.pow(10,6)
  val kHz=scala.math.pow(10,3)
  val timer=(CLK_FREQ_MHz*MHz/(SCCB_FREQ_KHz*kHz*2)).toInt

  val io=IO(new Bundle{
    val config          = Input(Bool())
    val sccb_ready      = Output(Bool())
    val SIOC            = Output(Bool())
    val SIOD            = Output(UInt(1.W))
    val config_data     = Input(UInt(8.W))
    val control_address = Input(UInt(8.W))
  })
  // as we only need to set up working modes for the camera, 3 phase write transmission cycle is employed
  // phase 1: ID Address, which is "h42" for OV7670
  // phase 2: sub-address, control registers inside the camera
  // phase 3: data to be overwritten to control registers inside OV7670 to specify working mode

  val OV7670_write_addr = "h42".U
  val sccb_ready        = WireInit(true.B)

  val latched_addr      = RegInit(0.U(8.W))
  val latched_data      = RegInit(0.U(8.W))

  val sccb_timer        = RegInit(0.U(32.W))
  val tx_byte           = RegInit(0.U(8.W))
  val byte_index        = RegInit(0.U(4.W))
  val byte_counter      = RegInit(0.U(2.W))
  val SIOC              = RegInit(false.B)
  val SIOD              = RegInit(false.B)
  val FMS_return_state  = RegInit(0.U(4.W))

  val fms_idle::fms_start::fms_load_byte::fms_tx_byte_low::fms_tx_byte_high::fms_index_check::fms_stop1::fms_stop2::fms_done::fms_sccb_timer::Nil = Enum(10)
  val FMS               = RegInit(fms_idle)

  io.sccb_ready := sccb_ready
  io.SIOD       := SIOD
  io.SIOC       := SIOC

  switch(FMS){
    is(fms_idle){
      SIOC := false.B     // bring SIOC, SIOD high
      SIOD := false.B
      sccb_ready:=true.B
      when(io.config){
        FMS        := fms_start
        sccb_ready := false.B
      }
      latched_data := io.config_data
      latched_addr := io.control_address
      tx_byte      := latched_data
      byte_index   := 0.U
      byte_counter := 0.U
    }
    is(fms_start){           // bring SIOC high, SIOD low
      sccb_ready       := false.B
      SIOC             := false.B
      SIOD             := true.B
      FMS              := fms_sccb_timer            // generate SIOC
      FMS_return_state := fms_load_byte
      sccb_timer       := timer.U
    }
    is(fms_load_byte){    // 1 clock
      sccb_ready := false.B
      FMS        := fms_tx_byte_low
      when(byte_counter===3.U ){
        FMS:=fms_stop1
      }
      byte_index   := 0.U
      byte_counter := byte_counter+1.U
      switch(byte_counter){
        is(0.U) { tx_byte := OV7670_write_addr}
        is(1.U) { tx_byte := latched_addr}
        is(2.U) { tx_byte := latched_data}
        is(3.U) { tx_byte := latched_data}
      }
    }
    is(fms_tx_byte_low){            // start inserting SIOD when SIOC is low for t_sccb/2
      sccb_ready       := false.B
      SIOC             := true.B
      FMS              := fms_sccb_timer     // data on low edge of SIOC
      FMS_return_state := fms_tx_byte_high
      sccb_timer       := timer.U
      SIOD             := Mux(byte_index===8.U,0.U,!tx_byte(7))
    }
    is(fms_tx_byte_high){       // keep SIOD same as when SIOC is high, finish one sccb clock for SIOD
      sccb_ready       := false.B
      SIOC             := false.B
      FMS              := fms_sccb_timer
      sccb_timer       := (timer/2).toInt.U
      FMS_return_state := fms_index_check
    }
    is(fms_index_check){
      sccb_ready       := false.B
      byte_index := byte_index+1.U
      tx_byte    := tx_byte<<1
      when(byte_index===8.U){           // when one byte is loaded successfully, along with dont care bit, load the next byte
        FMS := fms_load_byte
      }.otherwise{
        FMS := fms_tx_byte_low
      }
    }
    is(fms_stop1){  // bring SIOC high, SIOD low
      sccb_ready       := false.B
      SIOC             := false.B
      SIOD             := true.B
      sccb_timer       := timer.U
      FMS              := fms_sccb_timer
      FMS_return_state := fms_stop2
    }
    is(fms_stop2){  // bring SIOD high
      sccb_ready       := false.B
      SIOD             := false.B
      SIOC             := false.B
      sccb_timer       := timer.U
      FMS              := fms_sccb_timer
      FMS_return_state := fms_done
    }
    is(fms_done){ // add delay between transactions
      sccb_ready       := false.B
      sccb_timer       := timer.U
      FMS_return_state := fms_idle
      FMS              := fms_sccb_timer
    }
    is(fms_sccb_timer){           // delay state to create SIOC low and high
      sccb_ready       := false.B
      FMS := Mux(sccb_timer===0.U,FMS_return_state,FMS)
      sccb_timer := Mux(sccb_timer===0.U,0.U, sccb_timer-1.U)
    }
  }
}
