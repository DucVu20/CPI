package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._

class SCCBInterfaceTest(dut:SCCBInterface)(nOfRandomTest: Int) extends PeekPokeTester(dut: SCCBInterface){

  poke(dut.io.config,false)
  step(40)

  val numberOfTest = nOfRandomTest
  var numberOfTestsPassed = 0
  for (nTest<- 0 until numberOfTest){
    val slaveAddr=0x42
    val controlAddr = scala.util.Random.nextInt(255)
    val config_data  = scala.util.Random.nextInt(255)
    poke(dut.io.controlAddress, controlAddr)
    poke(dut.io.configData, config_data)
    poke(dut.io.config, false)

    step(scala.util.Random.nextInt(10))
    poke(dut.io.config,true)
    step(1)
    poke(dut.io.config,false)
    step(1)
    var dataBitIdx = 7
    val transmittedSlaveAddr = Array.fill(8){0}
    val transmittedAddr = Array.fill(8){0}
    val transmittedData = Array.fill(8){0}
    var phase = 0

    //Inverse the output of SIOD, and SIOC
    while(peek(dut.io.sccbReady)==0){

      var siocLow = !int2bool(peek(dut.io.SIOC))
      step(1)
      var siocHigh = !int2bool(peek(dut.io.SIOC))

      if(siocHigh-siocLow == 1){  //detect edge in SIOC
        var SIOD = !int2bool(peek(dut.io.SIOD))

        if(phase == 0 && (dataBitIdx != (-1))){
          transmittedSlaveAddr(dataBitIdx) = SIOD.toInt
        }
        else if(phase == 1 && (dataBitIdx!=(-1))){
          transmittedAddr(dataBitIdx) = SIOD.toInt
        }
        else if(phase==2 && (dataBitIdx != (-1))){
          transmittedData(dataBitIdx) = SIOD.toInt
        }
        dataBitIdx = dataBitIdx - 1

        if(dataBitIdx == (-2)){
          dataBitIdx = 7
          phase = phase + 1
        }
      }
    }

    step(50)
    // check for the number of tests passed
    if(slaveAddr == bin2dec(transmittedSlaveAddr)){
      if((controlAddr == bin2dec(transmittedAddr)) && (config_data==bin2dec(transmittedData))){
        numberOfTestsPassed = numberOfTestsPassed+1
      }
    }
  }
  Console.out.println(Console.YELLOW+"test result of SCCB interface: " + numberOfTestsPassed.toString+
    " tests passed over "+numberOfTest.toString+" being tested"+Console.RESET)

  def bin2dec(in : Array[Int]): Int={
    val arrayLength = in.length
    var dec = 0
    for(idx<-0 until arrayLength){
      dec = dec + in(idx) * scala.math.pow(2,idx).toInt
    }
    dec
  }

  def int2bool(int : BigInt): Boolean ={
    if(int==1){
      return true
    }
    else {
      return false
    }
  }
}

class SCCBInterfaceSpec extends FlatSpec with Matchers {
  "SCCB Interface" should "pass" in {
    chisel3.iotesters.Driver (() => new SCCBInterface(
      50,1000)) { c =>
      new SCCBInterfaceTest(c)(40)
    } should be (true)
  }
}