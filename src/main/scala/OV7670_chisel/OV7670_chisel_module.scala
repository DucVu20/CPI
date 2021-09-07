//package OV7670_chisel
//import chisel3._
//import chisel3.experimental.BaseModule
//import chisel3.util._
//
//
//case class CPI_Params(
//  address: BigInt = 0x2000,
//  width: Int = 32,
//  useAXI4: Boolean = false,
//
//  img_width: Int=640,
//  img_height: Int=480,
//  byte_per_pixel: Int,
//  baudRate: Int,
//  SCCB_FREQ: Int,
//  CLK_FREQ: Int,
//  pixel_width: Int=16
//  )
//
//class CPI_IO(val img_width: Int, val img_height: Int,
//             val byte_per_pixel: Int) extends  Bundle{
//
//  val clock= Input(Clock())
//  val reset=Input(Bool())
//
//  val read_frame = Input(Bool())
//  val frame_done = Output(Bool())
//  val buffer_full = Output(Bool())
//  val capture = Input(Bool())
//  // Capture frame Interface
//  val p_clk = Input(Bool())
//  val href = Input(Bool())
//  val vsync = Input(Bool())
//  val pixel_in = Input(UInt(8.W))
//  // SCCB interface
//  val SIOC = Output(Bool())
//  val SIOD = Output(UInt(1.W))
//  val start=Input(Bool())             // value from a button, when clicked, confirm the working mode to be selected
//  val ready=Output(Bool())
//  val config_data=Input(UInt(8.W))
//  val control_address=Input(UInt(8.W))
//  // data out io
//  val pixel_out=Output(UInt((8*byte_per_pixel).W))
//  val pixel_addr=Output(UInt((img_width*img_height).W))
//}
//
//trait HasCPI_IO extends BaseModule{
//  val img_width: Int
//  val img_height: Int
//  val byte_per_pixel: Int
//  val io=IO(new CPI_IO(img_width,img_height,byte_per_pixel))
//}
//
//class CPIModuleChisel(val img_width: Int, val img_height: Int,
//                      val byte_per_pixel: Int,
//                      val CLK_FREQ: Int, val SCCB_FREQ: Int) extends Module with HasCPI_IO{
//
//  def params: CPI_Params
//  val frame_capture = Module(new capture_frame_pixel_ver(img_width, img_height, byte_per_pixel))
//  val sccb_interface = Module(new SCCB_interface(params.CLK_FREQ, params.SCCB_FREQ))
//
//  io.p_clk:=frame_capture.io.p_clk
//  io.href:=frame_capture.io.href
//  io.vsync:=frame_capture.io.vsync
//  io.pixel_in:=frame_capture.io.data_in
//  io.pixel_out:=frame_capture.io.data_out
//  io.pixel_addr:=frame_capture.io.dout_addr
//
//  io.capture:=frame_capture.io.capture
//  io.frame_done:=frame_capture.io.frame_done
//  io.buffer_full:=frame_capture.io.buffer_full
//  io.read_frame:=frame_capture.io.read_frame  // to read a frame, read frame signal must be high
//                                              // for the entire frame
//  io.SIOC:=sccb_interface.io.SIOC
//  io.SIOD:=sccb_interface.io.SIOC
//  io.start:=sccb_interface.io.start
//  io.ready:=sccb_interface.io.ready
//  io.config_data:=sccb_interface.io.config_data
//  io.control_address:=sccb_interface.io.control_address
//
//}
//
//
//class CPIModuleChipYard(val img_width: Int, val img_height: Int,
//                        val byte_per_pixel: Int,
//                        val CLK_FREQ: Int, val SCCB_FREQ: Int
//                       )extends Module with HasCPI_IO {
//
//  val frame_capture = Module(new capture_frame_pixel_ver(img_width, img_height, byte_per_pixel))
//  val sccb_interface = Module(new SCCB_interface(CLK_FREQ, SCCB_FREQ))
//}
//trait CPIModule extends HasRegMap{
//  val io: CPI_IO
//
//  implicit val p: Parameters
//  def params: CPI_Params
//  val clock: Clock
//  val reset: Reset
//
//  val status= Wire(UInt(2.W))
//  val camera_config=WireInit(0.U(16.W))
//  val config=Wire(Bool())
//  val capture_frame=Wire(Bool())
//  val read_frame=Wire(Bool())
//  val pixel_out=Wire(UInt((params.byte_per_pixel*8).W))
//
//  CPI.io.control_address:=camera_config(15,8)
//  CPI.io.config_data:=camera_config(7,0)
//  CPI.io.capture:=capture_frame
//  CPI.io.read_frame:=read_frame
//  CPI.io.pixel_out:=pixel_out
//
//  val CPI=Module(new CPIModuleChisel(params.img_width,params.img_height,params.byte_per_pixel,
//                                    params.CLK_FREQ, params.SCCB_FREQ))
//
//  CPI.io.clock:=clock
//  CPI.io.reset:=reset
//  CPI.io.start:=config
//
////  CPI.io.p_clk:=io.p_clk
////  CPI.io.href:=io.href
////  CPI.io.vsync:=io.vsync
////  CPI.io.pixel_in:=io.pixel_in
////  CPI.io.pixel_out:=io.pixel_out
////  CPI.io.pixel_addr:=io.pixel_addr
////
////  CPI.io.capture:=io.pixel_addr
////  CPI.io.frame_done:=io.frame_done
////  CPI.io.buffer_full:=io.buffer_full
////  CPI.io.read_frame:=io.read_frame  // to read a frame, read frame signal must be high
////  CPI.io.SIOC:=io.SIOC
////  CPI.io.SIOC:=io.SIOD
//
//  status:=Cat(CPI.io.buffer_full,CPI.io.frame_done,CPI.io.ready)     // buffer_full goes high before frame_done is true
//
//  regmap(
//    0x00 -> Seq(
//      RegField.r(3,status)),    // read status of the camera
//    0x04 -> Seq(
//      RegField.w(16,camera_config)),    // write configuration to the cam
//    0x08 -> Seq(
//      RegField.w(1,config)),           // config the camera
//    0xC0 -> Seq(
//      RegField.w(1,capture_frame)),
//    0xC4 -> Seq(
//      RegField.w(1,read_frame)),
//    0xC8 -> Seq(
//      RegField.r(params.pixel_width,pixel_out)      // read frame
//    )
//
//    /// remember this version needs to modify the read frame signal in capture_frame module
//    // so that when the master device request a read, it will keep the read signal high until the
//    // buffer is empty
//  )
//}
//
//class CPI_AXI(params: CPI_Params, beatBytes: Int)(implicit p: Parameters)
//  extends AXI4RegisterRouter(
//    params.address,
//    beatBytes=beatBytes)(
//      new AXI4RegBundle(params, _) with GCDTopIO)(
//      new AXI4RegModule(params, _, _) with GCDModule)
//
//class WithCPI_AXI(useAXI4: Boolean ) extends Config((site,here, up) =>{
//
//})