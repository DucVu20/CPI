package cpi1

import chisel3._
import chisel3._
import chisel3.iotesters.{Driver, _}
import org.scalatest._
import cpi.CameraUartTop
import cpi.cpi.CameraUartTop


class UartCPITester(dut: CameraUartTop)(n: Int) extends PeekPokeTester(dut) {
  def software_tx(character:Int): Unit ={
    poke(dut.io.rx, 0)
    step(3)
    // 8 data bits
    for(i<- 0 until 8){
      poke(dut.io.rx,(character.toInt>>i)&0x01)
      step(3)
    }
    poke(dut.io.rx,1)
    while(peek(dut.io.valid)==0){
      step(1)
    }
  }
  def check_trasmitted_bytes(transmitted_byte: Int, data_name:String): Unit ={
    if(expect(dut.io.rx_bits,transmitted_byte)){
      println(data_name+" transmitted "+transmitted_byte.toString+" matched received "+data_name)
    }
    else {
      println(data_name+" transmitted is "+transmitted_byte.toString
        +" while received "+data_name+" is "+peek(dut.io.rx_bits).toString)
    }
  }
  def transmit_three_bytes(opcode: Int, addr: Int, data: Int): Unit ={
    software_tx(opcode)
    step(5)
    // check_trasmitted_bytes(opcode,"opcode")
    software_tx(addr)
    step(5)
    // check_trasmitted_bytes(addr, "address")
    software_tx(data)
    step(5)
    // check_trasmitted_bytes(data,"configuration data")
  }

  def getTransmittedBytes(message: String): Unit ={
    while(peek(dut.io.cpu_echo_valid)==0){
      step(1)
      //println("waiting")
    }
    poke(dut.io.cpu_ready,true.B)
    println(message+peek(dut.io.cpu_read_data).toInt.toHexString)
    step(1)
    poke(dut.io.cpu_ready,false.B)
    step(1)
  }
  def getPixel: Int ={
    while(peek(dut.io.cpu_echo_valid)==0){
      step(1)
    }
    poke(dut.io.cpu_ready,true.B)
    var pixel = peek(dut.io.cpu_read_data).toInt
    step(1)
    poke(dut.io.cpu_ready,false.B)
    step(1)
    return pixel
  }

  poke(dut.io.rx,true.B )
  step(300)

  val dontCare=0x7C
  println("Check sccb status, 1 is ready, otherwise")
  transmit_three_bytes(0x00,dontCare, dontCare)
  step(1)
  getTransmittedBytes("SCCB status is: ")

  println("configure working mode for the camera: addr=0x87, data=0x55")
  transmit_three_bytes(0x01,0x87,0x55)

  println("check camera's status")
  transmit_three_bytes(0x03,dontCare,dontCare)
  getTransmittedBytes("Camera status is: ")

  println("generate a capture signal and wait until vsync goes from high to low")
  transmit_three_bytes(0x02,dontCare,dontCare)

  val width=dut.width
  val height=dut.height
  val pclock=n
  println("generated a random frame")
  val tp=2*pclock
  val t_line=50*2*pclock
  //====================synthesized timing========================//
    val refFrame = new cpi.referenceFrame().generateRandomFrame(height * width, 1)

    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(10)
    step(2)
    poke(dut.io.vsync, true.B)
    poke(dut.io.href, false.B)
    step(3 * t_line)
    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(17 * t_line)
    var idx = 0
    var p_clk = true

  for (col <- 0 until width) {
    poke(dut.io.href, true.B)
    for (row <- 0 until height) {
      for (plk_clock <- 0 until (2)) {
        var pixelIn = new cpi.referenceFrame().pixelStream(idx, refFrame,
          1, plk_clock)

        poke(dut.io.href, true.B)
        poke(dut.io.vsync, false.B)
        p_clk = !p_clk
        poke(dut.io.p_clk, p_clk.asBool())
        if (p_clk == false) {
          poke(dut.io.pixel_in, pixelIn)
        }
        step(pclock / 2)
        p_clk = !p_clk
        poke(dut.io.p_clk, p_clk.asBool())
        step(pclock / 2)
      }
      idx = idx + 1
    }
    poke(dut.io.href, false.B)
    step(20 * tp)
  }
    step(1 * 120 * tp)
    poke(dut.io.vsync, true.B)
    step(1 * 120 * tp)
    //=========================validation============================//
    step(50)


  //===========================wait for 200 clock cycles, then read img from the buffer========/
  println("check buffer status")
  transmit_three_bytes(0x04,dontCare,dontCare)
  getTransmittedBytes("Buffer status is: ")

  step(500)
  println("read image from the buffer")
  transmit_three_bytes(0x05,dontCare,dontCare)
  var idx1=(0)
  var n_test_passed = 0
  for(a<- 0 until width*height-1){
    for(clk<- 0 until(2)){
      var refVal = new cpi.referenceFrame().pixelStream(idx1,refFrame,1,clk)
      val pixel = getPixel
     if(pixel==refVal){
       n_test_passed = n_test_passed + 1
     }
    }
    idx1=idx1+1
  }
  step(200)
  Console.out.println(Console.YELLOW+"test result of the CPI-UART is highlighted in blue")
  Console.out.println(Console.BLUE+(n_test_passed/2).toString+
    " pixels read via uart matched "+(width*height).toString+" reference pixels "+Console.RESET)

}


class UartCPISpec extends FlatSpec with Matchers {
  "Uart CPI test " should "pass" in {
    chisel3.iotesters.Driver(() => new CameraUartTop(
      0.01.toFloat,1,
      10,20,
      40*20,3000)) { c =>
      new UartCPITester(c)(4)
    } should be(true)
  }
}