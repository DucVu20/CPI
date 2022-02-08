package sislab.cpi

/////////////////////////////////////////////////////////////////////////////////////////
// This file integrate the I2C and the CaptureModule on a system on a chip on Chipyard //
//                  platform, developed at UC Berkerly.                                //
/////////////////////////////////////////////////////////////////////////////////////////

import chisel3._
import chisel3.util._
import chisel3.experimental.{IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

case class CPIParams(
  address          : BigInt  = 0x10020000,
  useAXI4          : Boolean = false,
  imgWidthCnt      : Int     = 640,
  imgHeightCnt     : Int     = 480,
  bytePerPixel     : Int     = 2,
  bufferDepth      : Int     = 352*290,
  maxXCLKPrescaler : Int     = 32
)

object CPIMMIO{
  val interfaceSetup   = 0x00
  val interfaceStatus  = 0x04
  val XCLKPrescaler    = 0x08
  val SCCBData         = 0x0C
  val capture          = 0x10
  val returnImgWidth   = 0x14
  val returnImgHeight  = 0x18
  val pixel            = 0x1C
  val pixelAddr        = 0x20
  val I2CPrescalerLow  = 0x24
  val I2CPrescalerHigh = 0x28
}

class CPIIO(val p: CPIParams) extends Bundle{

  val clock         = Input(Clock())
  val reset         = Input(Bool())
  val XCLK          = Output(Bool())
  val XCLKPrescaler = Input(UInt(log2Ceil(p.maxXCLKPrescaler).W))
  val activateXCLK  = Input(Bool())

  val pclk          = Input(Bool())
  val href          = Input(Bool())
  val vsync         = Input(Bool())
  val pixelIn       = Input(UInt(8.W))

  //RGB888 will be generated if you choose byterPerPixel to be 3
  val RGB888        = if(p.bytePerPixel == 3) Some(Input(Bool())) else None
  val capture       = Input(Bool())
  val videoMode     = Input(Bool()) // streaming consecutive frames when enabled 

  val pixelOut     = Output(UInt((8*p.bytePerPixel).W))
  val pixelAddr    = Output(UInt(log2Ceil(p.bufferDepth).W))
  val frameWidth   = Output(UInt(log2Ceil(p.imgWidthCnt).W))
  val frameHeight  = Output(UInt(log2Ceil(p.imgHeightCnt).W))
  val capturing    = Output(Bool())
  val newFrame     = Output(Bool()) // inform new frame, false when capture signal and vsync goes low
  val frameFull    = Output(Bool()) // interrupt, false when an entire frame is read out
  val readFrame    = Input(Bool())  // ready
  val pixelValid   = Output(Bool()) // valid
  val extractYComp = Input(Bool())

  val config         = Input(Bool())
  val coreEna        = Input(Bool())
  val SCCBReady      = Output(Bool())
  val SIOC           = Output(Bool())
  val SIOD           = Output(Bool())
  val configData     = Input(UInt(8.W))
  val controlAddress = Input(UInt(8.W))
  val prescalerLow   = Input(UInt(8.W))
  val prescalerHigh  = Input(UInt(8.W))
}

trait CPIPortIO extends Bundle{
  val SIOC    = Output(Bool())
  val SIOD    = Output(Bool())
  val pclk    = Input(Bool())
  val href    = Input(Bool())
  val vsync   = Input(Bool())
  val pixelIn = Input(UInt(8.W))
  val XCLK    = Output(Bool())
}
class CPI2IO extends Bundle{
  val SIOC    = Output(Bool())
  val SIOD    = Output(Bool())
  val pclk    = Input(Bool())
  val href    = Input(Bool())
  val vsync   = Input(Bool())
  val pixelIn = Input(UInt(8.W))
  val XCLK    = Output(Bool())
}

//class CPIInterrupts extends Bundle{
//  val newFrame = Bool()
//}

trait HasCPIIO extends BaseModule{
  val io = IO(new CPIIO(CPIParams.apply()))
}

class CPI(p: CPIParams) extends Module with HasCPIIO {

  val captureModule = Module(new CaptureModule(p.imgWidthCnt, p.imgHeightCnt,
    p.bytePerPixel, p.bufferDepth))
  val SCCBInterface = Module(new I2CMaster)
  val XCLKGenerator = Module(new XCLKSource(p.maxXCLKPrescaler))

  captureModule.io.pclk      := io.pclk
  captureModule.io.href      := io.href
  captureModule.io.vsync     := io.vsync
  captureModule.io.pixelIn   := io.pixelIn
  captureModule.io.capture   := io.capture
  captureModule.io.readFrame := io.readFrame  // to read a frame, read frame signal must be high
  captureModule.io.videoMode := io.videoMode
  if(p.bytePerPixel == 3){
    captureModule.io.RGB888.get  := io.RGB888.get
  }
  when(io.extractYComp){
    io.pixelOut    := captureModule.io.pixelOut(7, 0) // Y comp
  }.otherwise{
    io.pixelOut    := captureModule.io.pixelOut
  }
  io.pixelAddr   := captureModule.io.pixelAddr
  io.frameHeight := captureModule.io.frameHeight
  io.frameWidth  := captureModule.io.frameWidth
  io.capturing   := captureModule.io.capturing
  io.newFrame    := captureModule.io.newFrame
  io.frameFull   := captureModule.io.frameFull
  io.pixelValid  := captureModule.io.pixelValid

  XCLKGenerator.io.clockIn   := clock
  io.XCLK                    := XCLKGenerator.io.XCLK
  XCLKGenerator.io.prescaler := io.XCLKPrescaler
  XCLKGenerator.io.activate  := io.activateXCLK
  XCLKGenerator.io.reset     := io.reset

  io.SIOC                  := SCCBInterface.io.SIOC
  io.SIOD                  := SCCBInterface.io.SIOD
  io.SCCBReady             := SCCBInterface.io.SCCBReady
  SCCBInterface.io.coreEna := io.coreEna
  SCCBInterface.io.config        := io.config
  SCCBInterface.io.configData    := io.configData
  SCCBInterface.io.controlAddr   := io.controlAddress
  SCCBInterface.io.prescalerLow  := io.prescalerLow
  SCCBInterface.io.prescalerHigh := io.prescalerHigh
}
trait CPIModule extends HasRegMap{
  val io: CPIPortIO
  implicit val p: Parameters
  def params: CPIParams
  val clock: Clock
  val reset: Reset

  val pixel = Wire(new DecoupledIO(UInt((params.bytePerPixel*8).W)))
  val CPI   = Module(new CPI(params))

  val status        = Wire(UInt(5.W))
  val cameraMode    = Wire(DecoupledIO(UInt(16.W)))
  val pixelAddr     = Wire(UInt(CPI.io.pixelAddr.getWidth.W))
  val capture       = WireInit(false.B)
  val XCLKPrescaler = Reg(UInt(log2Ceil(params.maxXCLKPrescaler).W))
  val CPISetupReg   = Reg(UInt(5.W))
  val prescalerLow  = Reg(UInt(8.W))
  val prescalerHigh = Reg(UInt(8.W))
  //==== Cat(RGB888, CPI.io.videoMode, CPI.io.coreEna, CPI.io.activateXCLK)===//
  if(params.bytePerPixel == 3){
    CPI.io.RGB888.get := CPISetupReg(4)
  }
  CPI.io.extractYComp := CPISetupReg(3)
  CPI.io.videoMode    := CPISetupReg(2)
  CPI.io.coreEna      := CPISetupReg(1)
  CPI.io.activateXCLK := CPISetupReg(0)

  CPI.io.clock := clock
  CPI.io.reset := reset.asBool

  CPI.io.capture := capture
  CPI.io.pclk    := io.pclk
  CPI.io.href    := io.href
  CPI.io.vsync   := io.vsync
  CPI.io.pixelIn := io.pixelIn

  io.SIOC              := CPI.io.SIOC
  io.SIOD              := CPI.io.SIOD
  io.XCLK              := CPI.io.XCLK
  CPI.io.XCLKPrescaler := XCLKPrescaler

  CPI.io.controlAddress := cameraMode.bits(15,8)
  CPI.io.configData     := cameraMode.bits(7,0)
  CPI.io.config         := cameraMode.valid
  cameraMode.ready      := CPI.io.SCCBReady
  CPI.io.prescalerLow   := prescalerLow
  CPI.io.prescalerHigh  := prescalerHigh

  pixel.bits       := CPI.io.pixelOut
  pixel.valid      := CPI.io.pixelValid
  CPI.io.readFrame := pixel.ready
  pixelAddr        := CPI.io.pixelAddr
  // status: videomode, sccbready, frameFull, newFrame, capturing
  status := Cat(CPISetupReg(2), CPI.io.SCCBReady, CPI.io.frameFull, CPI.io.newFrame, CPI.io.capturing)
  // Memory Mapped Register
  regmap(
    CPIMMIO.interfaceSetup -> Seq(
      RegField.w(CPISetupReg.getWidth, CPISetupReg)),
    CPIMMIO.interfaceStatus -> Seq(
      RegField.r(status.getWidth, status)),
    CPIMMIO.XCLKPrescaler -> Seq(
      RegField.w(XCLKPrescaler.getWidth, XCLKPrescaler)),
    CPIMMIO.SCCBData -> Seq(
      RegField.w(16,cameraMode)),
    CPIMMIO.capture -> Seq(
      RegField.w(1, capture)),
    CPIMMIO.returnImgWidth -> Seq(
      RegField.r(CPI.io.frameWidth.getWidth,CPI.io.frameWidth)),
    CPIMMIO.returnImgHeight -> Seq(
      RegField.r(CPI.io.frameHeight.getWidth, CPI.io.frameHeight)),
    CPIMMIO.pixel -> Seq(
      RegField.r(16,pixel)),
    CPIMMIO.pixelAddr -> Seq(
      RegField.r(CPI.io.pixelAddr.getWidth, pixelAddr)
    ),
    CPIMMIO.I2CPrescalerLow -> Seq(
      RegField.w(8, prescalerLow)
    ),
    CPIMMIO.I2CPrescalerHigh -> Seq(
      RegField.w(8, prescalerHigh)
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
      val cpiPort=IO(new CPI2IO)

      cpiPort.SIOC          := cpi.module.io.SIOC
      cpiPort.SIOD          := cpi.module.io.SIOD
      cpi.module.io.vsync   := cpiPort.vsync
      cpi.module.io.href    := cpiPort.href
      cpi.module.io.pclk    := cpiPort.pclk
      cpi.module.io.pixelIn := cpiPort.pixelIn
      cpiPort.XCLK          := cpi.module.io.XCLK.asBool

      Some(cpiPort)
      //dontTouch(cpiPort)
    }
    case None => None
  }
}

class WithOV7670(useAXI4: Boolean) extends Config((site,here, up) => {
  case CPIKey => Some(CPIParams(useAXI4=useAXI4))
})
