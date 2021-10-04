package sislab.cpi

import chisel3._
import chisel3.util._

class ClockDivider(maxPrescaler: Int) extends Module{
  val io = IO(new Bundle {
    val clockIn      = Input(Clock())
    val dividedClock = Output(Bool())
    val reset        = Input(Bool())
    val prescaler    = Input(UInt(log2Ceil(maxPrescaler).W))
  })

  withClockAndReset(io.clockIn, io.reset){
    val max           = io.prescaler>>1
    val counter       = RegInit(0.U((io.prescaler.getWidth).W))
    val dividedClock  = RegInit(false.B)

    counter         := counter+1.U
    when(counter===(max.asUInt-1.U)){
      dividedClock  := (~dividedClock) // toggle
      counter       := 0.U
    }
    when(io.reset){
      counter      := 0.U
      dividedClock := false.B
    }
    io.dividedClock := dividedClock
  }
}

class XCLKSource(maxPrescaler: Int) extends Module{
  val io = IO(new Bundle{
    val clockIn      = Input(Clock())
    val XCLK         = Output(Bool())
    val reset        = Input(Bool())
    val prescaler    = Input(UInt(log2Ceil(maxPrescaler).W))
    val activateXCLK = Input(Bool())
  })
  val clockDivider = Module(new ClockDivider(maxPrescaler))

  clockDivider.io.clockIn   := io.clockIn
  clockDivider.io.reset     := io.reset
  clockDivider.io.prescaler := io.prescaler
  when(io.activateXCLK){
    io.XCLK  := clockDivider.io.dividedClock
  }otherwise{
    io.XCLK  := false.B
  }
}