package sislab.cpi

import chisel3._
import org.scalatest._
import chiseltest._

import chiseltest.internal.WriteVcdAnnotation
import chiseltest.experimental.TestOptionBuilder._

class referenceFrame{

  def generateRandomFrame(ImageResolution:Int, ImageFormat: Int): Array[Int]={
    if(ImageFormat == 1){ // gray
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(255)}
      return refFrame
    }
    else {
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
      return refFrame
    }
  }

  def pixelStream(idx:Int,refFrame: Array[Int],
                  ImageFormat:Int,
                  pclk: Int): Int ={
    if(ImageFormat==1){
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


class CaptureModuleChiselTest extends FlatSpec with ChiselScalatestTester{
  behavior of "Capture module"

  def CaptureModuleTest[T <: CaptureModule](dut: T, n :Int, imgWidth:Int, imgHeight:Int)={


    val pclock = n
    var nTestPassed = 0

    val tp = 2 * pclock
    val t_line = 2 * pclock
    //====================synthesized timing========================//
    println(" Capture frame with the resolution of "+imgWidth+"x"+imgHeight)
    for (imageFormat <- 0 until(2)) {
      dut.io.grayImage.poke(imageFormat.asUInt().asBool())
      println("generate a random frame")
      val refFrame = new referenceFrame().generateRandomFrame(imgWidth * imgHeight, imageFormat)

      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(10)
      dut.io.capture.poke(true.B)
      dut.clock.step(2)
      dut.io.capture.poke(false.B)
      dut.io.vsync.poke(true.B)
      dut.io.href.poke(false.B)
      dut.clock.step(3 * t_line)
      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(t_line)

      println("begin generating signals for format"+ (if(imageFormat==0) " RGB" else " Gray"))
      var idx = 0
      var pclk = true
      for (col <- 0 until(imgHeight)){
        dut.io.href.poke(true.B)
        for (row <- 0 until(imgWidth)){
          for (plkClock<- 0 until( if(imageFormat==0) 2 else 1)){
            var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
              imageFormat, plkClock)

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
      println("begin to validate captured frame")

      dut.clock.setTimeout(imgWidth*imgHeight*(imageFormat + 1) + 50)
      // this must be inserted if no input signals are changed for more than 1000 cycles
      // read all data from the buffer requires the number of cycles equal to the buffer's depth

      while (dut.io.frameFull.peek.litToBoolean) {
        dut.io.readFrame.poke(true.B)
        dut.clock.step(1)
        var idx_out = dut.io.pixelAddr.peek.litValue.toInt // pixel_address
        var refPixelVal = new referenceFrame().validate(idx_out, refFrame).toInt
        dut.io.pixelOut.expect(refPixelVal.U)
        if(refPixelVal==dut.io.pixelOut.peek.litValue.toInt){
          nTestPassed += 1
        }
      }
      println("colCnt returns: "+dut.io.frameWidth.peek.litValue().toInt+" frameWidth.getWidth "+dut.io.frameWidth.getWidth+
        " rowCnt: "+dut.io.frameHeight.peek.litValue().toInt +" frameHeight.getWidth "+dut.io.frameHeight.getWidth)

      dut.io.readFrame.poke(false.B)
      dut.clock.step(50)
    }
    Console.out.println(Console.YELLOW+"CPI testing result: "+nTestPassed+
      " tests passed over "+ Console.YELLOW+imgWidth*imgHeight*2+ " being tested"+
      Console.RESET)
  }

//  it should "pass" in {
//    test(new CaptureModule(64, 48, 2, 160*180))
//    { dut => CaptureModuleTest(dut,8)}
//  }

  "CaptureModule" should "pass" in{
    test(new CaptureModule(648,480,
      2,176*144)).withAnnotations(Seq(WriteVcdAnnotation)){
      dut => CaptureModuleTest(dut, 2, 176,144)
    }
  }
}