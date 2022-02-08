package sislab.cpi

////////////////////////////////////////////////////////////////////////////////////////////////
// I2C Specification desinged by Richard Herveille, redesigned and re-written in Chisel HDL   //
//                              by Vu D. Le                                                   //
//                          Date: September 2021                                              //
//             Company: Information Technology Institute, VNU                                 //
// Dedicated applications: Writing configurations to embedded camera having the I2C interface //
////////////////////////////////////////////////////////////////////////////////////////////////


// since the SCCB interface is the copy version of the I2C interface, and we're only
// interested in writing configurations to the camera. This interface is designed
// only for that purpose. Note that the clock divider above generate time pieces for
// the finite state machine to generate correct timing. To learn more above the I2C
// specifications, visit I2C-Master Core Specification - Richard Herveille for more
// information in timing specifications and control register. This design doesn't
//   cover everything in the I2C interface, designed to be lightweight.

import chisel3._
import chisel3.util._

class I2CMaster extends Module{
  val io = IO(new Bundle{
    val config        = Input(Bool())
    val SCCBReady     = Output(Bool())
    val SIOC          = Output(Bool())
    val SIOD          = Output(Bool())
    val configData    = Input(UInt(8.W))
    val controlAddr   = Input(UInt(8.W))
    val coreEna       = Input(Bool())
    val prescalerLow  = Input(UInt(8.W))
    val prescalerHigh = Input(UInt(8.W))
  })
  // as we only need to set up working modes for the camera, 3 phase write transmission cycle is employed
  // phase 1: ID Address, which is "h42" for OV7670
  // phase 2: sub-address, control registers inside the camera
  // phase 3: data to be overwritten to control registers inside OV7670 to specify working mode

  val SIOC         = RegInit(true.B)
  val SIOD         = RegInit(true.B)
  val clkCnt       = RegInit(0.U(18.W)) // counter for generating clock
  val clkEna       = RegInit(true.B)
  val latchedAddr  = RegInit(0.U(8.W))
  val latchedData  = RegInit(0.U(8.W))
  val SCCBReady    = RegInit(true.B)

  val transmitBit  = RegInit(false.B)

  val i2cWrite     = RegInit(false.B)
  val bitCnt       = RegInit(0.U(4.W))
  val byteCounter  = RegInit(0.U(2.W))
  val transmitByte = RegInit(0.U(8.W))

  when(io.coreEna){
    io.SIOC := SIOC
    io.SIOD := SIOD
  }.otherwise{
    io.SIOC := false.B
    io.SIOD := false.B
  }
  io.SCCBReady := SCCBReady

  // generate clock enable signal
  when( !io.coreEna || !(clkCnt.orR)){
    clkCnt := Cat(io.prescalerHigh, io.prescalerLow)
    clkEna := true.B
  }.otherwise{
    clkCnt := clkCnt -1.U
    clkEna := false.B
  }


  val (idle::bitIdle::bitStartA::bitStartB::bitStartC::bitStartD::bitStartE::
    bitStopA::bitStopB::bitStopC::bitStopD::
    bitWriteA::bitWriteB::bitWriteC::bitWriteD::loadByte::checkIndex::Nil ) = Enum(17)


  val FMS = RegInit(idle)
  transmitBit := Mux(!(bitCnt.orR), false.B , transmitByte(7))

  switch(FMS){
    is(idle) {
      SIOC        := true.B
      SIOD        := true.B
      latchedAddr := io.controlAddr
      latchedData := io.configData
      byteCounter := 3.U
      bitCnt      := 8.U
      when(io.config) {
        SCCBReady := false.B
        FMS := bitStartA
      }
    }
    is(bitIdle){
      when(clkEna){
        when(i2cWrite){
          FMS := checkIndex
        }
      }
    }
    is(bitStartA){
      when(clkEna){
        FMS  := bitStartB
        SIOC := true.B
        SIOD := true.B
      }
    }
    is(bitStartB){
      when(clkEna){
        FMS  := bitStartC
        SIOC := true.B
        SIOD := true.B
      }
    }
    is(bitStartC){
      when(clkEna){
        FMS  := bitStartD
        SIOC := true.B
        SIOD := false.B
      }
    }
    is(bitStartD){
      when(clkEna){
        FMS  := bitStartE
        SIOC := true.B
        SIOD := false.B
      }
    }
    is(bitStartE){
      when(clkEna){
        FMS  := loadByte
        SIOC := false.B
        SIOD := false.B
        i2cWrite := true.B
      }
    }
    is(bitStopA){
      when(clkEna){
        FMS  := bitStopB
        SIOC := false.B
        SIOD := false.B
      }
    }
    is(bitStopB){
      when(clkEna){
        FMS  := bitStopC
        SIOC := true.B
        SIOD := false.B
      }
    }
    is(bitStopC){
      when(clkEna){
        FMS  := bitStopD
        SIOC := true.B
        SIOD := false.B
      }
    }
    is(bitStopD){
      when(clkEna){
        FMS       := idle
        SIOC      := true.B
        SIOD      := true.B
        i2cWrite  := false.B
        SCCBReady := true.B
      }
    }
    is(bitWriteA){
      when(clkEna){
        FMS  := bitWriteB
        SIOC := false.B
      }
      SIOD := transmitBit
    }
    is(bitWriteB){
      when(clkEna){
        FMS  := bitWriteC
        SIOC := true.B
      }
      SIOD := transmitBit
    }
    is(bitWriteC){
      when(clkEna){
        FMS  := bitWriteD
        SIOC := true.B
      }
      SIOD := transmitBit
    }
    is(bitWriteD){
      when(clkEna){
        FMS  := bitIdle
        SIOC := false.B
      }
      SIOD := transmitBit
    }
    is(loadByte){
      bitCnt := 8.U
      byteCounter := byteCounter - 1.U
      when(!(byteCounter.orR)){
        FMS := bitStopA
      }.otherwise{
        FMS := bitWriteA
      }
      switch(byteCounter){
        is(3.U) { transmitByte := 0x42.U      } // address for writing, bit write included.
          // replace 0x42 by the latchedAddr if you want to write another address
        is(2.U) { transmitByte := latchedAddr }
        is(1.U) { transmitByte := latchedData }
        is(0.U) { transmitByte := latchedData }
      }
    }
    is(checkIndex){// check bit count and shift bit for transmitting
      bitCnt       := bitCnt - 1.U
      transmitByte := transmitByte << 1
      when(!(bitCnt.orR)){
        FMS := loadByte
      }.otherwise{
        FMS := bitWriteA
      }
    }
  }
}
