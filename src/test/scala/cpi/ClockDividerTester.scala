package sislab.cpi

import chisel3._
import chisel3.iotesters.{Driver, _}
import org.scalatest._


class ClockDividerTest(dut:ClockDividerDemo) extends PeekPokeTester(dut){

  //step(200)
  step(50)
    poke(dut.io.prescaler,6)
    step(16)
    poke(dut.io.reset,true.B)
    poke(dut.io.reset,false.B)
    step(1)
}

class ClockDividerWave extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new ClockDividerDemo(512)) { c =>
      new ClockDividerTest (c)
    } should be (true)
  }
}

