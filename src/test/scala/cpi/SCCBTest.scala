package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._

class SCCBInterfaceTest(dut:SCCBInterface)(n_of_random_test: Int) extends PeekPokeTester(dut: SCCBInterface){

  poke(dut.io.config,false)
  step(40)

  val number_of_test=(n_of_random_test)
  var number_of_tests_passed = 0
  for ( n_test<- 0 until number_of_test){
    val slave_addr=0x42
    val control_addr = scala.util.Random.nextInt(255)
    val config_data  = scala.util.Random.nextInt(255)
    poke(dut.io.controlAddress, control_addr)
    poke(dut.io.configData, config_data)
    poke(dut.io.config, false)

    step(scala.util.Random.nextInt(10))
    poke(dut.io.config,true)
    step(1)
    poke(dut.io.config,false)
    step(1)
    var data_bit_idx = 7
    val transmitted_slave_addr = Array.fill(8){0}
    val transmitted_addr = Array.fill(8){0}
    val transmitted_data = Array.fill(8){0}
    var phase = 0

    //Inverse the output of SIOD, and SIOC
    while(peek(dut.io.sccbReady)==0){

      var c_low = (!int2bool(peek(dut.io.SIOC).toInt))
      step(1)
      var c_high = (!int2bool(peek(dut.io.SIOC).toInt))

      if(c_high-c_low == 1){  //detect edge in SIOC
        var SIOD = (!int2bool(peek(dut.io.SIOD).toInt))

        if(phase == 0 && (data_bit_idx != (-1))){
          transmitted_slave_addr(data_bit_idx) = SIOD.toInt
        }
        else if(phase == 1 && (data_bit_idx!=(-1))){
          transmitted_addr(data_bit_idx) = SIOD.toInt
        }
        else if(phase==2 && (data_bit_idx != (-1))){
          transmitted_data(data_bit_idx) = SIOD.toInt
        }
        data_bit_idx = data_bit_idx-1

        if(data_bit_idx == (-2)){
          data_bit_idx = 7
          phase = phase+1
        }
      }
    }

    step(50)
    // check for the number of tests passed
    if(slave_addr == bin2dec(transmitted_slave_addr)){
      if((control_addr == bin2dec(transmitted_addr)) && (config_data==bin2dec(transmitted_data))){
        number_of_tests_passed = number_of_tests_passed+1
      }
    }
  }
  Console.out.println(Console.YELLOW+"testing results of the SCCB interface are highlighted in blue")
  Console.out.println(Console.BLUE+"test result: " +number_of_tests_passed.toString+
    " tests passed over "+number_of_test.toString+" being tested"+Console.RESET)

  def bin2dec(in:Array[Int]): Int={
    val array_length = in.length
    var dec = 0
    for(idx<-0 until array_length){
      dec = dec + in(idx) * scala.math.pow(2,idx).toInt
    }
    dec
  }

  def int2bool(int:Int): Boolean ={
    if(int==1){
      return true
    }
    else {
      return false
    }
  }
}

//class SCCB_interface_waveform extends FlatSpec with Matchers {
//  "WaveformCounter" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () =>
//      new SCCB_interface(50, 100.2 )) { c =>
//      new SCCB_interface_test(c)(20)
//    } should be (true)
//  }
//}

//object SCCBInterfaceTest extends App{
//  chisel3.iotesters.Driver(() => new SCCBInterface(50, 1000)){ c=>
//    new SCCBInterfaceTest(c)(50)
//  }
//}

class SCCBInterfaceSpec extends FlatSpec with Matchers {
  "SCCB Interface" should "pass" in {
    chisel3.iotesters.Driver (() => new SCCBInterface(
      50,1000)) { c =>
      new SCCBInterfaceTest(c)(200)
    } should be (true)
  }
}