package chipyard.CPI_test
import chisel3._
import chisel3.util._
import chisel3.iotesters._
import chisel3.iotesters.Driver
import CPI.{clock_divider,clock_divider_demo}
import org.scalatest._


class clock_divider_test (dut:clock_divider_demo) extends PeekPokeTester(dut){

  //step(200)
  step(50)
  for(i<-1 until 512){
    poke(dut.io.prescaler,i)
    step(2000)
    poke(dut.io.reset,true.B)
    step(200)
    poke(dut.io.reset,false.B)
    step(1)
  }
}

class clock_divider_CPI_waveform extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new clock_divider_demo(512)) { c =>
      new clock_divider_test (c)
    } should be (true)
  }
}
