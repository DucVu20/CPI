package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._
import chisel3.iotesters.Driver


class UartCPITester(dut: CameraUartTop)(n: Int) extends PeekPokeTester(dut) {
  def softwareTx(character:Int): Unit ={
    poke(dut.io.rx, 0)
    step(3)
    // 8 data bits
    for(i<- 0 until 8){
      poke(dut.io.rx, (character.toInt>>i)&0x01 )
      step(3)
    }
    poke(dut.io.rx, 1)
    while(peek(dut.io.valid.get)==0){
      step(1)
    }
  }
//  def check_trasmitted_bytes(transmitted_byte: Int, dataName:String): Unit ={
//    if(expect(dut.io.rxBits.get, transmitted_byte)){
//      println(dataName+" transmitted "+transmitted_byte.toString+" matched received "+dataName)
//    }
//    else {
//      println(dataName+" transmitted is "+transmitted_byte.toString
//        +" while received "+dataName+" is "+peek(dut.io.rxBits.get).toString)
//    }
//  }
  def transmitThreeBytes(opcode: Int, addr: Int, data: Int): Unit ={
    softwareTx(opcode)
    step(5)
    softwareTx(addr)
    step(5)
    softwareTx(data)
    step(5)
  }

  def getTransmittedBytes(message: String): Unit ={
    while(peek(dut.io.cpuEchoValid.get)==0){
      step(1)
      //println("waiting")
    }
    poke(dut.io.cpuReady.get, 1)
    println(message+peek(dut.io.cpuReadData.get).toInt.toHexString)
    step(1)
    poke(dut.io.cpuReady.get, 0)
    step(1)
  }
  def getPixel: Int ={
    while(peek(dut.io.cpuEchoValid.get) == 0){
      // wait on valid
      step(1)
    }
    var pixel = peek(dut.io.cpuReadData.get).toInt
    poke(dut.io.cpuReady.get, 1)
   // println("pixel: "+pixel)
    step(1)
    poke(dut.io.cpuReady.get, 1)
    step(1)
    return pixel
  }

  poke(dut.io.rx, 1 )
  step(300)

  val dontCare = 0x7C
  println("Check sccb status, 1 is ready, otherwise")
  transmitThreeBytes(0x00, dontCare, dontCare)
  step(1)
  getTransmittedBytes("SCCB status is: ")

  println("configure working mode for the camera: addr=0x87, data=0x55")
  transmitThreeBytes( 0x01, 0x87, 0x55)

  println("check camera's status")
  transmitThreeBytes( 0x03, dontCare, dontCare)
  getTransmittedBytes("Camera status is: ")

  println("generate a capture signal and wait until vsync goes from high to low")
  transmitThreeBytes( 0x02, dontCare, dontCare)

  val width = dut.width
  val height = dut.height
  val pclock = n
  println("generate a random frame")
  val tp = 2*pclock
  val t_line = 50*2*pclock
  //====================synthesized timing========================//
  val refFrame = new referenceFrame().generateRandomFrame(height * width, 1)

  poke(dut.io.vsync, false.B)
  poke(dut.io.href, false.B)
  step(10)
  step(2)
  poke(dut.io.vsync, true.B)
  poke(dut.io.href, false.B)
  step(3 * t_line)
  poke(dut.io.vsync, false.B)
  poke(dut.io.href, false.B)
  step(1 * t_line)
  var idx = 0
  var p_clk = true

  for (col <- 0 until width) {
    poke(dut.io.href, true.B)
    for (row <- 0 until height) {
      for (plk_clock <- 0 until (2)) {
        var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
          1, plk_clock)

        poke(dut.io.href, true.B)
        poke(dut.io.vsync, false.B)
        p_clk = !p_clk
        poke(dut.io.pclk, p_clk.asBool())
        if (p_clk == false) {
          poke(dut.io.pixel_in, pixelIn)
        }
        step(pclock / 2)
        p_clk = !p_clk
        poke(dut.io.pclk, p_clk.asBool())
        step(pclock / 2)
      }
      idx = idx + 1
    }
    poke(dut.io.href, false.B)
    step(20 * tp)
  }
  step(1 * 2 * tp)
  poke(dut.io.vsync, true.B)
  step(1 * 2 * tp)
  //=========================validation============================//
  step(50)


  //===========================wait for 200 clock cycles, then read img from the buffer========/
  println("check buffer status")
  transmitThreeBytes(0x04, dontCare, dontCare)
  getTransmittedBytes("Buffer status is: ")

  step(500)
  println("read image from the buffer")
  transmitThreeBytes(0x05, dontCare, dontCare)
  var idx1=(0)
  var n_test_passed = 0
  for(a<- 0 until width*height){
    for(clk<- 0 until(2)){
      var refVal = new referenceFrame().pixelStream(idx1, refFrame, 1, clk)
      val pixel = getPixel
      if(pixel==refVal){
        n_test_passed = n_test_passed + 1
      }
      else{
        println("Failed. Got: "+pixel+" expect: "+refVal)
      }
    }
    idx1=idx1+1
  }
  step(200)
  Console.out.println(Console.YELLOW+"test result of the CPI-UART is highlighted in blue")
  Console.out.println(Console.BLUE+(n_test_passed/2).toString+
    " pixels read via uart matched "+(width*height).toString+" reference pixels "+Console.RESET)

}
//class UartCPIWave extends FlatSpec with Matchers {
//  "Uart CPI waveform " should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () =>
//      new CameraUartTop(params.apply(0.01.toFloat,1,
//        30,30,40*30,3000,true))) { c =>
//      new UartCPITester(c)(4)
//    } should be (true)
//  }
//}

class UartCPISpec extends FlatSpec with Matchers {
  "Uart CPI test " should "pass" in {
    chisel3.iotesters.Driver(() => new CameraUartTop(params.apply(
      0.01.toFloat,1,
      12,12,40*40,3000,true))) { c =>
      new UartCPITester(c)(4)
    } should be(true)
  }
}

