package CPI
import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf
//import chipyard.iobinders.{OverrideIOBinder}

case class CPIParams(
                      address: BigInt         = 0x10003000,
                      useAXI4: Boolean        = false,
                      img_width: Int          = 640,
                      img_height: Int         = 480,
                      max_byte_per_pixel: Int = 2,
                      SCCB_FREQ_kHz: Double   = 100,
                      CLK_FREQ_MHz: Double    = 50,
                      pixel_width: Int        = 16,
                      max_prescaler: Int      = 32
                    )

object CPIMOMI{
  val cam_status                = 0x00   //wire
  val cam_capture               = 0x04
  val cam_mode                  = 0x08
  val cam_config                = 0x0C
  //val set_image_resolution      = 0x10
  val returned_image_resolution = 0x14
  //val read_frame              = 0x18
  val pixel                     = 0x18
  val prescaler                 = 0x1C
}

class CPIIO(val img_width: Int, val img_height: Int,
            val max_byte_per_pixel: Int,
            val max_prescaler: Int) extends Bundle{
  val frame_resolution = img_width*img_height // the maximum depth of the buffer
  val pixel_bits       = 8*max_byte_per_pixel

  val clock     = Input(Clock())
  val reset     = Input(Bool())
  val XCLK      = Output(Clock())
  val prescaler = Input(UInt(log2Ceil(max_prescaler).W))

  val p_clk          = Input(Bool())
  val href           = Input(Bool())
  val vsync          = Input(Bool())
  val pixel_in       = Input(UInt(8.W))
  val pixel_out      = Output(UInt(pixel_bits.W))
  val pixel_addr     = Output(UInt(log2Ceil(frame_resolution).W))
  val frame_width    = Output(UInt(log2Ceil(640).W))
  val frame_height   = Output(UInt(log2Ceil(480).W))
  val byte_per_pixel = Input(UInt(log2Ceil(max_byte_per_pixel).W))
  val capture        = Input(Bool())
  val capturing      = Output(Bool())
  val read_frame     = Input(Bool()) // ready
  val frame_full     = Output(Bool()) // valid

  val config          = Input(Bool())
  val sccb_ready      = Output(Bool())
  val SIOC            = Output(Bool())
  val SIOD            = Output(UInt(1.W))
  val config_data     = Input(UInt(8.W))
  val control_address = Input(UInt(8.W))
}
trait CPIPortIO extends Bundle{
  val SIOC     = Output(Bool())
  val SIOD     = Output(UInt(1.W))
  val p_clk    = Input(Bool())
  val href     = Input(Bool())
  val vsync    = Input(Bool())
  val pixel_in = Input(UInt(8.W))
  val XCLK     = Output(Clock())
}
class CPIInterrupts extends Bundle{
  val new_frame = Bool()
}

trait HasCPIIO extends BaseModule{
  val img_width     : Int
  val img_height    : Int
  val byte_per_pixel: Int
  val max_prescaler : Int
  val io = IO(new CPIIO(img_width,img_height,
    byte_per_pixel, max_prescaler))
}


class CPI(val img_width:  Int,
          val img_height: Int,
          val max_byte_per_pixel: Int,
          val CLK_FREQ_MHz: Double,
          val SCCB_FREQ_kHz: Double,
          val max_prescaler: Int ) extends Module with HasCPIIO {

  val capture_module = Module(new camera_module(img_width, img_height, max_byte_per_pixel))
  val SCCB_interface = Module(new SCCB_interface(CLK_FREQ_MHz, SCCB_FREQ_kHz))
  val XCLK_generator = Module(new clock_divider(max_prescaler))

  capture_module.io.p_clk          := io.p_clk
  capture_module.io.href           := io.href
  capture_module.io.vsync          := io.vsync
  capture_module.io.pixel_in       := io.pixel_in
  capture_module.io.capture        := io.capture
  capture_module.io.read_frame     := io.read_frame  // to read a frame, read frame signal must be high
  capture_module.io.byte_per_pixel := io.byte_per_pixel

  io.pixel_out    := capture_module.io.pixel_out
  io.pixel_addr   := capture_module.io.pixel_addr
  io.frame_height := capture_module.io.frame_height
  io.frame_width  := capture_module.io.frame_width
  io.capturing    := capture_module.io.capturing
  io.frame_full  := capture_module.io.frame_full

  XCLK_generator.io.clock_in := clock
  io.XCLK                    := XCLK_generator.io.divided_clock
  XCLK_generator.io.prescaler:= io.prescaler
  XCLK_generator.io.reset    := io.reset

  io.SIOC                    := SCCB_interface.io.SIOC
  io.SIOD                    := SCCB_interface.io.SIOC
  io.sccb_ready              := SCCB_interface.io.sccb_ready
  SCCB_interface.io.config          := io.config
  SCCB_interface.io.config_data     := io.config_data
  SCCB_interface.io.control_address := io.control_address
}
trait CPIModule extends HasRegMap{
  val io: CPIPortIO
  implicit val p: Parameters
  def params: CPIParams
  val clock: Clock
  val reset: Reset

  val pixel = Wire(new DecoupledIO(UInt((params.max_byte_per_pixel*8).W)))
  val CPI   = Module(new CPI(params.img_width, params.img_height,
    params.max_byte_per_pixel, params.CLK_FREQ_MHz,
    params.SCCB_FREQ_kHz, params.max_prescaler))

  val status              = Wire(UInt(3.W))
  val capture_frame       = WireInit(false.B)
  val config              = WireInit(false.B)
  val camera_mode         = WireInit(0.U(16.W))
  val pixel_addr          = Wire(UInt(16.W))
  val returned_resolution = Wire(UInt(19.W))
  val prescaler           = Wire(UInt(log2Ceil(params.max_prescaler).W))

  CPI.io.clock := clock
  CPI.io.reset := reset.asBool

  CPI.io.p_clk    := io.p_clk
  CPI.io.href     := io.href
  CPI.io.vsync    := io.vsync
  CPI.io.pixel_in := io.pixel_in

  io.SIOC := CPI.io.SIOC
  io.SIOD := CPI.io.SIOC

  io.XCLK := CPI.io.XCLK
  CPI.io.prescaler:=prescaler

  CPI.io.control_address := camera_mode(15,8)
  CPI.io.config_data     := camera_mode(7,0)
  CPI.io.capture         := capture_frame
  CPI.io.config          := config


  pixel.bits  := CPI.io.pixel_out
  pixel.valid := CPI.io.frame_full
  pixel_addr  := CPI.io.pixel_addr

  status := Cat(CPI.io.sccb_ready, CPI.io.frame_full, CPI.io.capturing)

  CPI.io.read_frame   := pixel.ready
  returned_resolution := Cat(CPI.io.frame_width,CPI.io.frame_height)

  regmap(
    CPIMOMI.cam_status -> Seq(
      RegField.r(3,status)),
    CPIMOMI.cam_capture -> Seq(
      RegField.w(1,capture_frame)),
    CPIMOMI.cam_mode -> Seq(
      RegField.w(16,camera_mode)),
    CPIMOMI.cam_config -> Seq(
      RegField.w(1,config)),
    CPIMOMI.returned_image_resolution -> Seq(
      RegField.r(19,returned_resolution)
    ),
    CPIMOMI.pixel -> Seq(
      RegField.r(16,pixel)),
    CPIMOMI.prescaler -> Seq(
      RegField.w(log2Ceil(params.max_prescaler),prescaler)
    )
  )
}

class CPITL(params: CPIParams,beatBytes: Int)(implicit  p:Parameters)
  extends TLRegisterRouter(
    params.address,"OV7670", Seq("ucbbar,OV7670"),
    beatBytes=beatBytes)(
    new TLRegBundle(params, _) with CPIPortIO)(
    new TLRegModule(params, _, _) with CPIModule)

class CPIAXI4(params: CPIParams, beatBytes: Int)(implicit p: Parameters)

  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
    new AXI4RegBundle(params, _) with CPIPortIO)(
    new AXI4RegModule(params, _, _) with CPIModule)

case object CPIKey extends Field[Option[CPIParams]](None)

trait CanHavePeripheryCPI {this : BaseSubsystem =>
  private val portName= "OV7670"

  val cpi=p(CPIKey) match{
    case Some(params) => {
      if (params.useAXI4) {
        val cpi = LazyModule(new CPIAXI4(params, pbus.beatBytes)(p))
        pbus.toSlave(Some(portName)) {
          cpi.node :=
            AXI4Buffer () :=
            TLToAXI4 () :=
            // toVariableWidthSlave doesn't use holdFirstDeny, which TLToAXI4() needsx
            TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
        }
        Some(cpi)
      } else {
        val cpi = LazyModule(new CPITL(params, pbus.beatBytes)(p))
        pbus.toVariableWidthSlave(Some(portName)) { cpi.node }
        Some(cpi)
      }
    }
    case None => None
  }
}

trait CanHavePeripheryCPIModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryCPI
  val port = outer.cpi match {
    case  Some(cpi) => {
      val cpi_port=IO(new Bundle{
        val SIOC     = Output(Bool())
        val SIOD     = Output(Bool())
        val vsync    = Input(Bool())
        val href     = Input(Bool())
        val pclk     = Input(Bool())
        val pixel_in = Input(UInt(8.W))
        val XCLK     = Output(Clock())
      })

      cpi_port.SIOC          := cpi.module.io.SIOC
      cpi_port.SIOD          := cpi.module.io.SIOD
      cpi.module.io.vsync    := cpi_port.vsync
      cpi.module.io.href     := cpi_port.href
      cpi.module.io.p_clk    := cpi_port.pclk
      cpi.module.io.pixel_in := cpi_port.pixel_in
      cpi_port.XCLK          := cpi.module.io.XCLK

      Some(cpi_port)
    }
    case None => None
  }
}

class WithOV7670(useAXI4: Boolean) extends Config((site,here, up) => {
  case CPIKey => Some(CPIParams(useAXI4=useAXI4))
})

