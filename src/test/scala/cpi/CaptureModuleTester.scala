package sislab.cpi

import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.internal.WriteVcdAnnotation
import chiseltest.experimental.TestOptionBuilder._
import scala.math.pow
import scala.io.Source


class referenceFrame{

  def generateRandomFrame(ImageResolution:Int, ImageFormat: String): Array[Int]={
    if(ImageFormat == "RGB888"){ // RGB 24 bit
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(pow(2,24).toInt)}
      return refFrame
    }
    else { // RGB 16 bit
      val refFrame = Array.fill(ImageResolution){scala.util.Random.nextInt(65535)}
      return refFrame
    }
  }

  def pixelStream(idx:Int,refFrame: Array[Int],
                  ImageFormat:String,
                  pclk: Int): Int ={
    if(ImageFormat == "RGB888"){
      val pixel = refFrame(idx)
      val firstByte  = pixel >> 16
      val secondByte = (pixel & 0xFF00) >> 8
      val thirdByte  = pixel & 0xFF
      if (pclk == 0) {
        return firstByte
      }
      else if (pclk == 1) {
        return secondByte
      }
      else{
        return thirdByte
      }
    }
    else {
      var firstByte  = refFrame(idx) >> 8
      var secondByte = refFrame(idx) & 0xFF
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

  def CaptureModuleTest[T <: CaptureModule](dut: T, n :Int, imgWidth:Int, imgHeight:Int,
                                            bytePerPixel: Int, nTest: Int,
                                            testRGB565onRGB88HW: Boolean)= {

    val pclock = n
    var nTestFailed = 0

    val tp = 2 * pclock
    val t_line = 2 * pclock
    val imageFormat = if(!testRGB565onRGB88HW) {if(bytePerPixel == 3) "RGB888"
    else "16bitRGB"} else "16bitRGB"
    if(bytePerPixel == 3){
      if(testRGB565onRGB88HW) dut.io.RGB888.get.poke(false.B) else dut.io.RGB888.get.poke(true.B)
    }
    else None

    //====================synthesized timing========================//
    for(n <- 0 until nTest){
      val refFrame = new referenceFrame().generateRandomFrame(imgWidth * imgHeight, imageFormat)

      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(10)
      dut.io.capture.poke(true.B)
      dut.clock.step(1)
      dut.io.capture.poke(false.B)
      dut.io.vsync.poke(true.B)
      dut.io.href.poke(false.B)
      dut.clock.step(3 * t_line)
      dut.io.vsync.poke(false.B)
      dut.io.href.poke(false.B)
      dut.clock.step(t_line)

      //println("begin generating signals for "+ imageFormat)
      var idx = 0
      var pclk = true
      for (col <- 0 until (imgWidth)) {
        dut.io.href.poke(true.B)
        for (row <- 0 until (imgHeight)) {
          for (plkClock <- 0 until (if(imageFormat == "RGB888") 3 else 2)) {
            var pixelIn = new referenceFrame().pixelStream(idx, refFrame,
              imageFormat, plkClock)

            dut.io.href.poke(true.B)
            dut.io.vsync.poke(false.B)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            if (pclk == false) {
              dut.io.pixelIn.poke(pixelIn.asUInt())
            }
            dut.clock.step(pclock / 2)
            pclk = !pclk
            dut.io.pclk.poke(pclk.asBool())
            dut.clock.step(pclock / 2)
          }
          idx = idx + 1
        }
        dut.io.href.poke(false.B)
        dut.clock.step(2 * tp)
      }

      // this part is meant to validate that when the capture signal is not triggered
      // the interface wouldn't capture when vsync changes
      for (a <- 0 until 4) {
        dut.clock.step(tp)
        dut.io.vsync.poke(true.B)
        dut.clock.step(50)
        dut.io.vsync.poke(false.B)
        dut.clock.step(50)
        for (a <- 0 until 5) {
          dut.io.href.poke(true.B)
          dut.clock.step(50)
          dut.io.href.poke(false.B)
          dut.clock.step(50)
        }
        dut.io.vsync.poke(true.B)
        dut.clock.step(50)
        dut.io.vsync.poke(false.B)
        dut.clock.step(50)
      }
      dut.io.vsync.poke(false.B)
      dut.io.capture.poke(true.B)
      dut.clock.step(2)
      dut.io.capture.poke(false.B)

      //====================validation=======================//
      //println("begin to validate captured frame")

      if ((imgWidth * imgHeight) < dut.depth) {

        dut.clock.setTimeout(imgWidth * imgHeight * 2 + 50)
        // this must be inserted if no input signals are changed for more than 1000 cycles
        // read all data from the buffer requires the number of cycles equal to the buffer's depth
        while (dut.io.frameFull.peek.litToBoolean) {
          dut.io.readFrame.poke(true.B)
          dut.clock.step(1)
          var idx_out = dut.io.pixelAddr.peek.litValue.toInt // pixel_address
          if(dut.io.pixelValid.peek().litToBoolean) {
            var refPixelVal = new referenceFrame().validate(idx_out, refFrame).toInt

            dut.io.pixelOut.expect(refPixelVal.U)
          }
        }

        Console.out.println(Console.BLUE+ " TEST "+ (if(bytePerPixel == 2) " 16bit RGB hardware "
        else " 24bit RGB hardware "))
        // validate resolution counter
        Console.out.println(Console.YELLOW+"colCnt returns: " + dut.io.frameWidth.peek.litValue().toInt + " expect " + imgWidth +
          " rowCnt: " + dut.io.frameHeight.peek.litValue().toInt + " expect " + imgHeight + " at test: "+n)
        // print out image format being tested on RGB? hardware
        Console.out.println(Console.YELLOW + "CPI testing result of "+ imageFormat+
          (if(bytePerPixel == 3) { (if(testRGB565onRGB88HW) " on RGB888 hardware passed " else " passed" )}))
        dut.io.readFrame.poke(false.B)
        dut.clock.step(50)
      }
      else {
        Console.out.println(Console.YELLOW+"colCnt returns: " + dut.io.frameWidth.peek.litValue().toInt + " expect " + imgWidth +
          " rowCnt: " + dut.io.frameHeight.peek.litValue().toInt + " expect " + imgHeight)
        println(Console.RED + "frame transmitted to the interface exceeds buffer size")
      }
    }
    Console.out.println(Console.RESET)
    dut.clock.step(300)
  }
  it should "RGB16bit on RGB16 HW" in {
    test(new CaptureModule(40, 40, 2, 160*180))
    { dut => CaptureModuleTest(dut,2, 30, 30,2, 4, false )}
  }
  it should "RGB888 on RGB24 HW" in {
    test(new CaptureModule(40, 40, 3, 160*180))
    { dut => CaptureModuleTest(dut,2, 30, 30,3, 4, false )}
  }
  it should "RGB16bit on RGB24HW" in {
    test(new CaptureModule(20, 20, 3, 160*180))
    { dut => CaptureModuleTest(dut,2, 30, 30,3, 4, true )}
  }

  //  "CaptureModule wave" should "pass" in{
  //    test(new CaptureModule(60,60,
  //      3,50*50)).withAnnotations(Seq(WriteVcdAnnotation)){
  //      dut => CaptureModuleTest(dut, 2, 20,20,3, 4)
  //    }
  //  }
}

class CaptureModuleChiselTestRealFrame extends FlatSpec with ChiselScalatestTester {
  behavior of "Capture module"

  def CaptureModuleTestDogFrame[T <: CaptureModule](dut: T, n: Int, imgWidth: Int, imgHeight: Int,
                                            bytePerPixel: Int, nTest: Int
                                            ) = {

    val pclock = n
    var nTestFailed = 0

    val tp = 2 * pclock
    val t_line = 2 * pclock

    //====================synthesized timing========================//
    val dogFrame = Source.fromFile("E:/HDL/CHISEL projects/CPI/src/test/resources/dogGray.txt").getLines.toArray

    dut.io.vsync.poke(false.B)
    dut.io.href.poke(false.B)
    dut.clock.step(10)
    dut.io.capture.poke(true.B)
    dut.clock.step(1)
    dut.io.capture.poke(false.B)
    dut.io.vsync.poke(true.B)
    dut.io.href.poke(false.B)
    dut.clock.step(3 * t_line)
    dut.io.vsync.poke(false.B)
    dut.io.href.poke(false.B)
    dut.clock.step(t_line)

    //println("begin generating signals for "+ imageFormat)
    var idx = 0
    var pclk = true
    for (col <- 0 until (imgWidth)) {
      dut.io.href.poke(true.B)
      for (row <- 0 until (imgHeight)) {
        for (plkClock <- 0 until (2)) {

          dut.io.href.poke(true.B)
          dut.io.vsync.poke(false.B)
          pclk = !pclk
          dut.io.pclk.poke(pclk.asBool())
          if (pclk == false) {
            dut.io.pixelIn.poke(dogFrame(idx).toInt.asUInt())
          }
          dut.clock.step(pclock / 2)
          pclk = !pclk
          dut.io.pclk.poke(pclk.asBool())
          dut.clock.step(pclock / 2)
        }
        idx = idx + 1
      }
      dut.io.href.poke(false.B)
      dut.clock.step(2 * tp)
    }

    // this part is meant to validate that when the capture signal is not triggered
    // the interface wouldn't capture when vsync changes
    for (a <- 0 until 4) {
      dut.clock.step(tp)
      dut.io.vsync.poke(true.B)
      dut.clock.step(50)
      dut.io.vsync.poke(false.B)
      dut.clock.step(50)
      for (a <- 0 until 5) {
        dut.io.href.poke(true.B)
        dut.clock.step(50)
        dut.io.href.poke(false.B)
        dut.clock.step(50)
      }
      dut.io.vsync.poke(true.B)
      dut.clock.step(50)
      dut.io.vsync.poke(false.B)
      dut.clock.step(50)
    }
    dut.io.vsync.poke(false.B)
    dut.io.capture.poke(true.B)
    dut.clock.step(2)
    dut.io.capture.poke(false.B)

    //====================validation=======================//
    //println("begin to validate captured frame")

    dut.clock.setTimeout(imgWidth * imgHeight * 2 + 50)
    // this must be inserted if no input signals are changed for more than 1000 cycles
    // read all data from the buffer requires the number of cycles equal to the buffer's depth
    while (dut.io.frameFull.peek.litToBoolean) {
      dut.io.readFrame.poke(true.B)
      dut.clock.step(1)
      var idx_out = dut.io.pixelAddr.peek.litValue.toInt // pixel_address
      // print out the YUV value of the photo and apply the 0xFF mask before recovering the
      // the result back to gray image. The python function in the test/resources will
      // help you recover these pixel values back to the orginal image (which is a dog)
      if (dut.io.pixelValid.peek.litToBoolean) {
        print(dut.io.pixelOut.peek().litValue().toInt.toHexString + " ")
      }
    }
    println(" ")
  }
  it should "RGB16bit on RGB16 HW" in {
    test(new CaptureModule(640, 480, 2, 259 * 194)) {
      dut => CaptureModuleTestDogFrame(dut, 2, 259, 194, 2, 4) }
  }
}
