package chipyard.CPI_test

import CPI._
import chisel3._
import chisel3.iotesters.Driver
import org.scalatest._
import chisel3.iotesters._

class capture_module_tester(dut:camera_module)(n:Int,imageFormat: Int) extends PeekPokeTester(dut) {

  val height = dut.h
  val width = dut.w

  // To run the simulation, n must be greater than 1
  val pclock = n
  val tp = 2 * pclock
  val t_line = 784 * 2 * pclock

  //====================synthesized timing========================//

  for (imageFormat <- 0 until (2)) {
    poke(dut.io.imageFormat, imageFormat)
    val refFrame = new referenceFrame().generateRandomFrame(height * width, imageFormat)

    poke(dut.io.vsync, false.B)
    poke(dut.io.href, false.B)
    step(10)
    poke(dut.io.capture, true.B)
    step(2)
    poke(dut.io.capture, false.B)
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
        for (plk_clock <- 0 until (imageFormat + 1)) {
          var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
            imageFormat, plk_clock)

          poke(dut.io.href, true.B)
          poke(dut.io.vsync, false.B)
          p_clk = !p_clk
          poke(dut.io.pclk, p_clk.asBool())
          if (p_clk == false) {
            poke(dut.io.pixelIn, pixelIn)
          }
          step(pclock / 2)
          p_clk = !p_clk
          poke(dut.io.pclk, p_clk.asBool())
          step(pclock / 2)
        }
        idx = idx + 1
      }
      poke(dut.io.href, false.B)
      step(144 * tp)
    }
    step(10 * 784 * tp)
    poke(dut.io.vsync, true.B)
    step(1 * 784 * tp)
    //=========================validation============================//

    while (peek(dut.io.frame_full) == 1) {
      step(scala.util.Random.nextInt(10))
      poke(dut.io.read_frame, true.B)
      step(1)

      var idx_out = peek(dut.io.pixelAddr).toInt // pixel_address
      var refPixelVal = new referenceFrame().validate(idx_out, refFrame)

      if (imageFormat == 1) {
        expect(dut.io.pixelOut, refPixelVal)
      }
      else {
        expect(dut.io.pixelOut, refPixelVal)
      }
      poke(dut.io.read_frame, false.B)
      //step(1)
    }
    step(200)
  }
}
class referenceFrame(){

  def generateRandomFrame(ImageResolution:Int, ImageFormat: Int): Array[Int]={
    if(ImageFormat==0){
      val refFrame=Array.fill(ImageResolution){scala.util.Random.nextInt(255)}
      return refFrame
    }
    else {
      val refFrame=Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
      return refFrame
    }
  }

  def pixelStream(idx:Int,refFrame: Array[Int],
                  ImageFormat:Int,
                  pclk: Int): Int ={
    if(ImageFormat==0){
      return refFrame(idx)
    }
    else {
      var firstByte = refFrame(idx)>>8
      var secondByte = refFrame(idx)&0xFF
      if (pclk == 0) {
        return firstByte
      }
      else {
        return secondByte
      }
    }
  }

  def validate(idx: Int , refFrame: Array[Int]): Int={
    return refFrame(idx)
  }
}

class wave_of_capture_module extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new camera_module(20,10)){ c =>
      new capture_module_tester(c)(4,0)
    } should be (true)
  }
}

object capture_module_tester extends App{
  chisel3.iotesters.Driver(() => new camera_module(20,10)){ c=>
    new capture_module_tester(c)(10,0)
  }
}