package sislab.cpi

import chisel3._
import chisel3.iotesters._
import org.scalatest._
import chisel3.iotesters.Driver
import chiseltest._


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

  poke(dut.io.vsync, 0)
  poke(dut.io.href, 0)
  step(10)
  step(2)
  poke(dut.io.vsync, 1)
  poke(dut.io.href, 0)
  step(3 * t_line)
  poke(dut.io.vsync, 0)
  poke(dut.io.href, 0)
  step(1 * t_line)
  var idx = 0
  var p_clk = true

  for (col <- 0 until width) {
    poke(dut.io.href, 1)
    for (row <- 0 until height) {
      for (plk_clock <- 0 until (2)) {
        var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
          1, plk_clock)

        poke(dut.io.href, 1)
        poke(dut.io.vsync, 0)
        p_clk = !p_clk
        poke(dut.io.pclk, p_clk.asBool())
        if (p_clk == false) {
          poke(dut.io.pixelIn, pixelIn)
        }
        step(pclock / 2)
        p_clk = !p_clk
        poke(dut.io.pclk, p_clk.asBool().toInt)
        step(pclock / 2)
      }
      idx = idx + 1
    }
    poke(dut.io.href, 0)
    step(20 * tp)
  }
  step(1 * 2 * tp)
  poke(dut.io.vsync, 1)
  step(1 * 2 * tp)
  //=========================validation============================//
  step(50)


  //===========================wait for 200 clock cycles, then read img from the buffer========/
  println("check buffer status")
  transmitThreeBytes(0x04, dontCare, dontCare)
  getTransmittedBytes("Buffer status is: ")

  step(300)
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
class UartCPIWave extends FlatSpec with Matchers {
  "Uart CPI waveform " should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new CameraUartTop(params.apply(0.01.toFloat,1,
        30,30,40*30,3000,true))) { c =>
      new UartCPITester(c)(4)
    } should be (true)
  }
}

class UartCPISpec extends FlatSpec with Matchers {
  "Uart CPI test " should "pass" in {
    chisel3.iotesters.Driver(() => new CameraUartTop(params.apply(
      0.01.toFloat,1,
      10,12,60*60,3000,true))) { c =>
      new UartCPITester(c)(4)
    } should be(true)
  }
}
class UarCPITest extends FlatSpec with ChiselScalatestTester{
  behavior of "CPI-UART"

//  "UART CPI chisel tester wave " should "pass" in {
//    test(new CameraUartTop(params.apply(0.01.toFloat,1,
//      60,60,60*60,3000,true)))
  def CpiUartTest[T <: CameraUartTop](dut: T, n :Int)={

    def softwareTx(character:Int): Unit ={
      dut.io.rx.poke(0.U)
      dut.clock.step(3)
      // 8 data bits
      for(i<- 0 until 8){
        dut.io.rx.poke(((character.toInt>>i)&0x01).asUInt())
        dut.clock.step(3)
      }
      dut.io.rx.poke(1.U)
      while (dut.io.valid.get == false){
        dut.clock.step(1)
      }
    }

    def transmitThreeBytes(opcode: Int, addr: Int, data: Int): Unit ={
      softwareTx(opcode)
      dut.clock.step(5)
      softwareTx(addr)
      dut.clock.step(5)
      softwareTx(data)
      dut.clock.step(5)
    }

    def getTransmittedBytes(message: String): Unit ={
      while(dut.io.cpuEchoValid.get.peek.litToBoolean == false){
        dut.clock.step(1)
        //println("waiting")
      }
      dut.io.cpuReady.get.poke(true.B)

      println(message+dut.io.cpuReadData.get.peek().litValue().toInt.toHexString)
      dut.clock.step(1)
      dut.io.cpuReady.get.poke(false.B)
      dut.clock.step(1)
    }
    def getPixel: Int ={
      while(dut.io.cpuEchoValid.get.peek.litToBoolean == false){
        // wait on valid
        dut.clock.step(1)
      }
      var pixel = dut.io.cpuReadData.get.peek.litValue().toInt
      dut.io.cpuReady.get.poke(true.B)
      dut.clock.step(1)
      dut.io.cpuReady.get.poke(false.B)
      dut.clock.step(1)

      return pixel
    }

    dut.io.rx.poke(1.U)
    dut.clock.step(200)

    val dontCare = 0x7C
    println("Check sccb status, 1 is ready, otherwise")
    transmitThreeBytes(0x00, dontCare, dontCare)
    dut.clock.step(1)
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
    val tp = 2 * pclock
    val t_line = 10 * pclock
    //====================synthesized timing========================//
    val refFrame = new referenceFrame().generateRandomFrame(height * width, 1)

    dut.io.vsync.poke(false.B)
    dut.io.href.poke(false.B)
    dut.clock.step(10)
    dut.io.vsync.poke(true.B)
    dut.io.href.poke(false.B)
    dut.clock.step(3*t_line)
    dut.io.vsync.poke(false.B)
    dut.io.href.poke(false.B)
    dut.clock.step(t_line)

    var idx = 0
    var pclk = true
    for (col <- 0 until(width)){
      dut.io.href.poke(true.B)
      for (row <- 0 until(height)){
        for (plkClock<- 0 until(2)){
          var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
            1, plkClock)

          dut.io.href.poke(true.B)
          dut.io.vsync.poke(false.B)
          pclk = !pclk
          dut.io.pclk.poke(pclk.asBool())
          if (pclk == false){
            dut.io.pixelIn.poke(pixelIn.asUInt())
          }
          dut.clock.step(pclock/2)
          pclk = !pclk
          dut.io.pclk.poke(pclk.asBool())
          dut.clock.step(pclock/2)
        }
        idx = idx + 1
      }
      dut.io.href.poke(false.B)
      dut.clock.step(2*tp)
    }
    dut.clock.step(tp)
    dut.io.vsync.poke(true.B)
    dut.clock.step(tp)

    //====================validation=======================//
    dut.clock.step(20)

    println("check buffer status")
    transmitThreeBytes(0x04, dontCare, dontCare)
    getTransmittedBytes("Buffer status is: ")

    dut.clock.step(100)
    println("read image from the buffer")
    transmitThreeBytes(0x05, dontCare, dontCare)

    var idx1 = 0
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
    dut.clock.step(100)

    Console.out.println(Console.YELLOW+"test result of the CPI-UART is highlighted in blue")
    Console.out.println(Console.BLUE+(n_test_passed/2).toString+
      " pixels read via uart matched "+(width*height).toString+" reference pixels "+Console.RESET)
  }
  it should "pass" in {
    test(new CameraUartTop(params.apply(0.01.toFloat,1,
      10,20,60*60,3000,true))) { dut => CpiUartTest(dut, 4)}
  }
}

