package sislab.cpi_test

import sislab.cpi._
import chisel3._
import chisel3.iotesters.Driver
import org.scalatest._
import chisel3.iotesters._

import java.io._
import scala.io.Source
import scala.util.control._


class capture_module_tester(dut:camera_module)(n:Int) extends PeekPokeTester(dut){
  val random_frame_ov7670=Source.fromResource("random_frame_0v7670_ver1.txt").getLines.toArray   // width*height=640x480
  val pixel_val_frame_of_the_random_frame=Source.fromResource("pixel_val_frame_ver1.txt").getLines.toArray
  val width=dut.w
  val height=dut.h
  // To run the simulation, n must be greater than 1
  val pclock=n
  val tp=2*pclock
  val t_line=784*2*pclock

  //====================synthesized timing========================//
  // poke(dut.io.frame_depth_in,120*90)
  poke(dut.io.vsync,false.B)
  poke(dut.io.href,false.B)
  step(10)
  poke(dut.io.capture,true.B)
  step(2)
  poke(dut.io.capture,false.B)
  poke(dut.io.vsync,true.B)
  poke(dut.io.href,false.B)
  step(3*t_line)
  poke(dut.io.vsync,false.B)
  poke(dut.io.href,false.B)
  step(17*t_line)
  // first pixel starts when h_ref goes high
  var idx = 0
  var p_clk = true
  for(col <-0 until width){
    poke(dut.io.href,true.B)              // href goes high for one row and goes low for 144_tp
    for(row<-0 until height){
      for(plk_clock<-0 until 2){
        poke(dut.io.href,true.B)
        poke(dut.io.vsync,false.B)
        p_clk = !p_clk
        poke(dut.io.p_clk,p_clk.asBool())
        if(p_clk==false){
          poke(dut.io.pixel_in,random_frame_ov7670(idx).toInt)
        }
        step(pclock/2)
        p_clk = !p_clk
        poke(dut.io.p_clk,p_clk.asBool())
        if(p_clk==false){
          poke(dut.io.pixel_in,random_frame_ov7670(idx).toInt)
        }
        step(pclock/2)
        idx+=1
      }
    }
    poke(dut.io.href,false.B)
    step(144*tp)                  // href goes low for 144tp before going high
  }
  step(10*784*tp)
  poke(dut.io.vsync, true.B)
  step(3*784*tp)

  //=========================validation============================//


  val loop= new Breaks
  loop.breakable{
    while(peek(dut.io.frame_full)==1){
      poke(dut.io.read_frame,false.B)
      step(scala.util.Random.nextInt(10))
      poke(dut.io.read_frame,true.B)
      step(1)
      var idx_out=peek(dut.io.pixel_addr).toInt    // pixel_address
      expect(dut.io.pixel_out,pixel_val_frame_of_the_random_frame(idx_out).toInt)
    }
    if(peek(dut.io.frame_full)==0){
      println("testing is done")
      step(5)
      loop.break;
    }
  }
  step(200)
}
class wave_of_frame_capture_ver1 extends FlatSpec with Matchers {
  "WaveformCounter" should "pass" in {
    Driver.execute(Array("--generate-vcd-output", "on"), () =>
      new camera_module(120,90,2)){ c =>
      new capture_module_tester(c)(10)
    } should be (true)
  }
}

object frame_capture_pixel_ver extends App{
  chisel3.iotesters.Driver(() => new camera_module(120,90,2)){ c=>
    new capture_module_tester(c)(10)
  }
}
