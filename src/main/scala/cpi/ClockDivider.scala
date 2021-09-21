package sislab.cpi

import chisel3._
import chisel3.util._

class ClockDivider(maxPrescaler: Int) extends Module{
  val io = IO(new Bundle {
    val clockIn      = Input(Clock())
    val dividedClock = Output(Clock())
    val reset         = Input(Bool())
    val prescaler     = Input(UInt(log2Ceil(maxPrescaler).W))
  })
  val counterWidth = io.prescaler.getWidth

  withClockAndReset(io.clockIn, io.reset){
    val max           = io.prescaler>>1
    val counter       = RegInit(0.U(counterWidth.W))
    val divided_clock = RegInit(false.B)

    counter         := counter+1.U
    when(counter===(max.asUInt-1.U)){
      divided_clock := (~divided_clock) // toggle
      counter       := 0.U
    }
    when(io.reset){
      counter       := 0.U
      divided_clock := false.B
    }
    io.dividedClock:= divided_clock.asClock()
  }
}

class ClockDividerDemo(maxPrescaler:Int) extends Module{
  val io = IO(new Bundle {
    val reset         = Input(Bool())
    val dividedClock = Output(Clock())
    val prescaler     = Input(UInt(log2Ceil(maxPrescaler).W))
  })
  val clk_div=Module(new ClockDivider(maxPrescaler))
  clk_div.io.clockIn :=clock
  clk_div.io.reset    :=io.reset
  clk_div.io.prescaler:=io.prescaler
  io.dividedClock    :=clk_div.io.dividedClock

  println(clk_div.io.dividedClock.name)
}