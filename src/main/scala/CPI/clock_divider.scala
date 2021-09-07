package CPI

import chisel3._
import chisel3.util._

class clock_divider(max_prescaler: Int) extends Module{
  val io=IO(new Bundle{
    val clock_in      = Input(Clock())
    val divided_clock = Output(Clock())
    val reset         = Input(Bool())
    val prescaler     = Input(UInt(log2Ceil(max_prescaler).W))
  })
  val counter_width = io.prescaler.getWidth

  withClockAndReset(io.clock_in, io.reset){
    val max           = io.prescaler>>1
    val counter       = RegInit(0.U(counter_width.W))
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
    io.divided_clock:= divided_clock.asClock()
  }
}

class clock_divider_demo(max_prescaler:Int) extends Module{
  val io=IO(new Bundle{
    val reset         = Input(Bool())
    val divided_clock = Output(Clock())
    val prescaler     = Input(UInt(log2Ceil(max_prescaler).W))
  })
  val clk_div=Module(new clock_divider(max_prescaler))
  clk_div.io.clock_in :=clock
  clk_div.io.reset    :=io.reset
  clk_div.io.prescaler:=io.prescaler
  io.divided_clock    :=clk_div.io.divided_clock
}
