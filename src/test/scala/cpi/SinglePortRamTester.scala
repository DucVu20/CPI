package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._

class SinglePortRamTester(dut:SinglePortRam[UInt]) extends PeekPokeTester(dut){
  val maxDataInputVal = scala.math.pow(2, dut.io.dataIn.getWidth).toInt
  val memDepth = dut.depth
  val randomInputVal=Array.fill(memDepth){scala.util.Random.nextInt(maxDataInputVal)}

  for (a<- 0 until memDepth){
    poke(dut.io.addr,a)
    poke(dut.io.dataIn,randomInputVal(a))
    poke(dut.io.wrEna,true.B)
    poke(dut.io.rdEna,false.B)
    step(1)
  }
  step(5) // wait util stable
  for(a<-0 until memDepth){
    poke(dut.io.addr,a)
    poke(dut.io.wrEna,false.B)
    poke(dut.io.rdEna,true.B)
    step(1)
    expect(dut.io.dataOut,randomInputVal(a))
  }
}

//object SinglePortRamTester extends App{
//  chisel3.iotesters.Driver(()=> new SinglePortRam( 4020, UInt(16.W))){ c=>
//    new SinglePortRamTester(c)
//  }
//}

class SinglePortRamSpec extends FlatSpec with Matchers {
  "Single Port Ram" should "pass" in {
    chisel3.iotesters.Driver (() => new SinglePortRam(
      4020, UInt(16.W))) { c =>
      new SinglePortRamTester(c)
    } should be (true)
  }
}