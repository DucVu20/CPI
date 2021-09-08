package CPI

import chisel3._

import scala.Array._

class OV7670_config_rom(circuits_working_freq: Int, t_bounce: Float) extends Module{
  /**
   * as this design use a button to select working modes for the OV7670, it's of paramount importance
   * to filter out bouncing and detect state transition ( from 0 to 1) to move to next working mode.
   * In order to sample correctly, t_sample must be greater than bouncing time
   */
  val FAC=(t_bounce*circuits_working_freq/100+100).toInt  // add 100 clk to ensure that bouncing is not present
  val io=IO(new Bundle{
    val control_address=Output(UInt(8.W))
    val config_data=Output(UInt(8.W))
    val select_mode=Input(Bool())
  })
  val addr      = RegInit(0.U(8.W))
  val btnDebReg = Reg(Bool())
  val cntReg    = RegInit(0.U(32.W))

  val tick=cntReg===(FAC-1).U
  cntReg:=cntReg+1.U
  when(tick){
    cntReg:=0.U
    btnDebReg:=io.select_mode         // sample after bouncing is over
  }
  when(btnDebReg& !RegNext(btnDebReg)){
    addr:=addr+1.U
    when(addr===73.U){
      addr:=0.U
    }
  }
  val array= new Array[Int](256)
  array(0)  =  "h12_80".toInt//reset
  array(1)  =  "hFF_F0".toInt //delay
  array(2)  =  "h12_04".toInt // COM7,     set RGB color output
  array(3)  =  "h11_80".toInt // CLKRC     internal PLL matches input clock
  array(4)  =  "h0C_00".toInt // COM3,     default settings
  array(5)  =  "h3E_00".toInt // COM14,    no scaling, normal pclock
  array(6)  =  "h04_00".toInt // COM1,     disable CCIR656
  array(7)  =  "h40_d0".toInt //COM15,     RGB565, full output range
  array(8)  =  "h3a_04".toInt //TSLB       set correct output data sequence (magic)
  array(9)  =  "h14_18".toInt //COM9       MAX AGC value x4
  array(10) =  "h4F_B3".toInt //MTX1       all of these are magical matrix coefficients
  array(11) =  "h50_B3".toInt //MTX2
  array(12) =  "h51_00".toInt //MTX3
  array(13) =  "h52_3d".toInt //MTX4
  array(14) =  "h53_A7".toInt //MTX5
  array(15) =  "h54_E4".toInt //MTX6
  array(16) =  "h58_9E".toInt //MTXS
  array(17) =  "h3D_C0".toInt //COM13      sets gamma enable, does not preserve reserved bits, may be wrong?
  array(18) =  "h17_14".toInt //HSTART     start high 8 bits
  array(19) =  "h18_02".toInt //HSTOP      stop high 8 bits //these kill the odd colored line
  array(20) =  "h32_80".toInt //HREF       edge offset
  array(21) =  "h19_03".toInt //VSTART     start high 8 bits
  array(22) =  "h1A_7B".toInt //VSTOP      stop high 8 bits
  array(23) =  "h03_0A".toInt //VREF       vsync edge offset
  array(24) =  "h0F_41".toInt //COM6       reset timings
  array(25) =  "h1E_00".toInt //MVFP       disable mirror / flip //might have magic value of 03
  array(26) =  "h33_0B".toInt //CHLF       //magic value from the internet
  array(27) =  "h3C_78".toInt //COM12      no HREF when VSYNC low
  array(28) =  "h69_00".toInt //GFIX       fix gain control
  array(29) =  "h74_00".toInt //ARRAY() = 74      Digital gain control
  array(30) =  "hB0_84".toInt //RSVD       magic value from the internet *required* for good color
  array(31) =  "hB1_0c".toInt //ABLC1
  array(32) =  "hB2_0e".toInt //RSVD       more magic internet values
  array(33) =  "hB3_80".toInt //THL_ST
  //begin mystery scaling numbers
  array(34) =  "h70_3a".toInt
  array(35) =  "h71_35".toInt
  array(36) =  "h72_11".toInt
  array(37) =  "h73_f0".toInt
  array(38) =  "ha2_02".toInt
  //gamma curve values
  array(39) =  "h7a_20".toInt
  array(40) =  "h7b_10".toInt
  array(41) =  "h7c_1e".toInt
  array(42) =  "h7d_35".toInt
  array(43) =  "h7e_5a".toInt
  array(44) =  "h7f_69".toInt
  array(45) =  "h80_76".toInt
  array(46) =  "h81_80".toInt
  array(47) =  "h82_88".toInt
  array(48) =  "h83_8f".toInt
  array(49) =  "h84_96".toInt
  array(50) =  "h85_a3".toInt
  array(51) =  "h86_af".toInt
  array(52) =  "h87_c4".toInt
  array(53) =  "h88_d7".toInt
  array(54) =  "h89_e8".toInt
  //AGC and AEC
  //is(54.U) array() =  "h13_e0".U //COM8, disable AGC / AEC
  array(55) =  "h00_00".toInt //set gain array() =  to 0 for AGC
  array(56) =  "h10_00".toInt //set ARCJ array() =  to 0
  array(57) =  "h0d_40".toInt //magic reserved bit for COM4
  array(58) =  "h14_18".toInt //COM9, 4x gain + magic bit
  array(59) =  "ha5_05".toInt // BD50MAX
  array(60) =  "hab_07".toInt //DB60MAX
  array(61) =  "h24_95".toInt //AGC upper limit
  array(62) =  "h25_33".toInt //AGC lower limit
  array(63) =  "h26_e3".toInt //AGC/AEC fast mode op array() = ion
  array(64) =  "h9f_78".toInt //HAECC1
  array(65) =  "ha0_68".toInt //HAECC2
  array(66) =  "ha1_03".toInt //magic
  array(67) =  "ha6_d8".toInt //HAECC3
  array(68) =  "ha7_d8".toInt //HAECC4
  array(69) =  "ha8_f0".toInt //HAECC5
  array(70) =  "ha9_90".toInt //HAECC6
  array(71) =  "haa_94".toInt //HAECC7
  array(72) =  "h13_e5".toInt //COM8, enable AGC / AEC

  val camera_config=VecInit(array.map(_.U(16.W)))         // create a LUT
  io.control_address:=camera_config(addr)(15,8)
  io.config_data:=camera_config(addr)(7,0)

}

