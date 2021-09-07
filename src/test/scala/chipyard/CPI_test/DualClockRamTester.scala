package chipyard.CPI_test

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import chisel3.iotesters.Driver
import org.scalatest._
import CPI.DualClockRamDemo

import scala.util.Random
import scala.math.pow
import CPI.DualClockRam

//class DualClockTester(dut:DualClockRam[UInt]) extends PeekPokeTester(dut){
//  step(20)
//  val bufferDepth = dut.depth
//  val dataBitWidth = dut.io.dataIn.getWidth.toInt
//  val referenceData = Array.fill(bufferDepth)
//  {Random.nextInt(pow(2, dataBitWidth).toInt)}
//
//  var wrClock=false
//  for(i<- 0 until bufferDepth){
//    poke(dut.io.wrAddr,i)
//    poke(dut.io.dataIn,referenceData(i))
//    poke(dut.io.wrEna,true)
//  }
//
//}

class DualClockRamTester(dut: DualClockRamDemo[UInt]) extends PeekPokeTester(dut){
  step(50)
  val bufferDepth = dut.dualClockRam.depth
  val dataBitWidth = dut.dualClockRam.io.dataIn.getWidth.toInt
  val maxPrescaler  = pow(2,dut.clockGenerator.io.prescaler.getWidth.toInt).toInt
  val referenceData = Array.fill(bufferDepth)
  {Random.nextInt(pow(2, dataBitWidth).toInt)}

  // assume reading frequency = 20 x writing frequency
  val prescaler = 20
  poke(dut.io.preScaler,prescaler)
  val href = true

  while(peek(dut.io.pclk)==1){
    step(1)
  }
  //==============write to the buffer=================//
  for(i <- 0 until bufferDepth){
    poke(dut.io.wrAddr,i)
    for(j<- 0 until prescaler){
      var pclk=peek(dut.io.pclk)
      poke(dut.io.dataIn,referenceData(i))
      poke(dut.io.wrEna, href)
      step(1)
    }
  }
  poke(dut.io.wrEna,false.B)
  step(20)
  //=================read and verify ===================//
  for(a<- 0 until bufferDepth){
    poke(dut.io.readAddr,a)
    step(1)
    expect(dut.io.dataOut,referenceData(a))
  }

}
class WaveformDualClockRAM extends FlatSpec with Matchers {
  "WaveformSinglePortRAM" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"),
      () => new DualClockRamDemo(1024, UInt(16.W))) {
      c => new DualClockRamTester(c)
    } should be (true)
  }
}
object TestSinglePortUIntRAM extends App {
  chisel3.iotesters.Driver(() =>
    new DualClockRamDemo(1024, UInt(8.W))) {
    c => new DualClockRamTester(c)
  }
}