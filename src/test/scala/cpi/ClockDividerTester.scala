package sislab.cpi

import chisel3._
import chisel3.iotesters.{Driver, _}
import org.scalatest._


class ClockDividerTest(dut: XCLKSource) extends PeekPokeTester(dut){

  //step(200)
  poke(dut.io.activate, false.B)
  poke(dut.io.prescaler, 6)
  step(5000)
  poke(dut.io.activate, true.B)
  poke(dut.io.prescaler, 20)
  step(15000)
  poke(dut.io.prescaler,4)
  step(3000)
  step(1)

  poke(dut.io.prescaler, 10)
  step(1000)

  poke(dut.io.activate, false.B)
  step(1000)
}

class ClockDividerWave extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new XCLKSource(512)) { c =>
      new ClockDividerTest (c)
    } should be (true)
  }
}