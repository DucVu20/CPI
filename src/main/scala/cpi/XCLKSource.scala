package sislab.cpi

import chisel3._
import chisel3.util._

class XCLKSource(maxPrescaler: Int) extends Module{
  val io = IO(new Bundle{
    val clockIn      = Input(Clock())
    val XCLK         = Output(Bool())
    val prescaler    = Input(UInt(log2Ceil(maxPrescaler).W))
    val activate     = Input(Bool())
    val reset        = Input(Bool())
  })
  val clockDivider = Module(new ClockDivider(maxPrescaler))

  clockDivider.io.clockIn   := io.clockIn
  clockDivider.io.reset     := io.reset
  clockDivider.io.prescaler := io.prescaler
  clockDivider.io.activate  := io.activate
  io.XCLK                   := clockDivider.io.dividedClock
}

class ClockDivider(maxPrescaler: Int) extends Module{
  val io = IO(new Bundle {
    val clockIn      = Input(Clock())
    val dividedClock = Output(Bool())
    val reset        = Input(Bool())
    val prescaler    = Input(UInt(log2Ceil(maxPrescaler).W))
    val activate    = Input(Bool())
  })

  withClockAndReset(io.clockIn, io.reset){
    val counter       = RegInit(0.U((io.prescaler.getWidth).W))
    val dividedClock  = RegInit(false.B)

    when(!io.activate || !(counter.orR)){
      counter := ((io.prescaler>>1).asUInt() -1.U)
      dividedClock := ~dividedClock
    }.otherwise{
      counter := counter - 1.U
    }
    when(io.activate){
      io.dividedClock := dividedClock
    }.otherwise{
      io.dividedClock := false.B
    }
  }
}