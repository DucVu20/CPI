package OV7670_chisel
import chisel3._
import chisel3.util._

import chisel3.iotesters.Driver
import chisel3.iotesters.PeekPokeTester
import org.scalatest._

class SCCB_interface(CLK_FREQ: Int, SCCB_FREQ: Int) extends Module{

  val timer=CLK_FREQ/(SCCB_FREQ*2)    // value for toggling between low and high.
  //val FAC=(CLK_FREQ/(t_bounce_ms*1000)+1000).toInt    // value for timer to sample button's state after bouncing is over

  val io=IO(new Bundle{
    val start=Input(Bool())             // value from a button, when clicked, confirm the working mode to be selected
    val ready=Output(Bool())
    val SIOC=Output(Bool())
    val SIOD=Output(UInt(1.W))
    val config_data=Input(UInt(8.W))
    val control_address=Input(UInt(8.W))
  })
  // as we only need to set up working modes for the camera, 3 phase write transmission cycle is employed
  // phase 1: ID Address, which is "h42" for OV7670
  // phase 2: sub-address, control registers inside the camera
  // phase 3: data to be overwritten to control registers inside OV7670 to specify working mode
  val OV7670_write_addr="h42".U
  //val OV7670_read_aadr="h43".U
  val ready=RegInit(true.B)
  val latched_addr=RegInit(0.U(8.W))
  val latched_data=RegInit(0.U(8.W))
  val sccb_timer=RegInit(0.U(32.W))
  val tx_byte=RegInit(0.U(8.W))
  val byte_index=RegInit(0.U(4.W))
  val byte_counter=RegInit(0.U(2.W))
  val SIOC=RegInit(false.B)
  val SIOD=RegInit(false.B)
  val FMS_return_state=RegInit(0.U(3.W))
  val fms_idle::fms_start::fms_load_byte::fms_tx_byte_low::fms_tx_byte_high::fms_index_check::fms_sccb_timer::fms_stop::Nil=Enum(8)
  val FMS=RegInit(fms_idle)

  io.ready:=ready
  io.SIOD:=SIOD
  io.SIOC:=SIOC
  switch(FMS){
    is(fms_idle){
      SIOC:=true.B
      SIOD:=true.B
      ready:=true.B
      when(io.start){
        FMS:=fms_start
        ready:=false.B
      }
      latched_data:=io.config_data
      latched_addr:=io.control_address
      tx_byte:=latched_data
      byte_index:=0.U
      byte_counter:=0.U
    }
    is(fms_start){           // bring SIOC high, SIOD low for t_sccb/2
      SIOC:=true.B
      SIOD:=false.B
      FMS:=fms_sccb_timer            // generate SIOC
      FMS_return_state:=fms_load_byte
      sccb_timer:=timer.U
    }
    is(fms_load_byte){    // 1 clock
      FMS:=fms_tx_byte_low
      when(byte_counter===3.U ){
        FMS:=fms_stop
      }
      byte_index:=0.U
      byte_counter:=byte_counter+1.U
      switch(byte_counter){
        is(0.U) { tx_byte:=OV7670_write_addr}
        is(1.U) { tx_byte:=latched_addr}
        is(2.U) {tx_byte:=latched_data}
        is(3.U) {tx_byte:=latched_data}
      }
    }
    is(fms_tx_byte_low){        // start inserting SIOD when SIOC is low for t_sccb/2
      SIOC:=false.B
      FMS:=fms_sccb_timer     // data on low edge of SIOC
      FMS_return_state:=fms_tx_byte_high
      sccb_timer:=timer.U
      SIOD:=Mux(byte_index===8.U,0.U,tx_byte(7))
    }
    is(fms_tx_byte_high){       // keep SIOD same as when SIOC is high, finish one sccb clock for SIOD
      SIOC:=true.B
      FMS:=fms_sccb_timer
      sccb_timer:=timer.U
      SIOD:=tx_byte(7)
      FMS_return_state:=fms_index_check
    }
    is(fms_index_check){
      byte_index:=byte_index+1.U
      tx_byte:=tx_byte<<1
      when(byte_index===8.U){           // when one byte is loaded successfully, along with dont care bit, load the next byte
        FMS:=fms_load_byte
      }.otherwise{
        FMS:=fms_tx_byte_low
      }
    }
    is(fms_stop){
      SIOC:=true.B
      SIOD:=true.B
      sccb_timer:=timer.U
      FMS:=fms_sccb_timer
      FMS_return_state:=fms_idle
    }
    is(fms_sccb_timer){           // delay state to create SIOC low and high
      FMS:=Mux(sccb_timer===0.U,FMS_return_state,FMS)
      sccb_timer:=Mux(sccb_timer===0.U,0.U, sccb_timer-1.U)
    }
  }
}
