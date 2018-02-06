/*******************************************************/
/*              Includes                               */
/*******************************************************/

#include "nexus_includes.h"
#include "tdal_av.h"
#include "tdal_av_priv.h"
#include "tdal_av_module_priv.h"

#include "tbox.h"
#include "tdal_common_priv.h"
#include "tdal_common.h"
#include "tkel.h"
#include "tdal_disp.h"

//--------bla

#include "tdal_dmx_module_priv.h"

/*******************************************************/
/*              Defines                                */
/*******************************************************/

#define NUM_VIDEO_DECODERS ( 1 )
#define NUM_AUDIO_DECODERS ( 1 )
#define NUM_STC_CHANNELS NUM_AUDIO_DECODERS

#define auto __auto_type

/********************************************************
   *   Macros                        *
********************************************************/

mTBOX_SET_MODULE(eTDAL_AV);

/********************************************************
   *   Global Variables   (GLOBAL)            *
********************************************************/

/********************************************************
   *   Local   File   Variables   (LOCAL)            *
********************************************************/

/** Indicates whether AV module is initialized. 
Only functions for getting capabilities can be caled if module is not initialized **/
LOCAL uint8_t gTDAL_AV_isInitialized = false;

/** Local structure for storing params related to video decoder **/
LOCAL tTDAL_AV_VideoHandle gVideoDecoder;

/** Local structure for storing params related to audio decoder **/
LOCAL tTDAL_AV_AudioHandle gAudioDecoder;

/*******************************************************/
/*           Local Functions Declarations              */
/******************************************************/

/** Functions for getting initialized audio and video decoder.
Local structures gVideoDecoder and gAudioDecoder should not be accessed directly, 
but via these functions **/
LOCAL tTDAL_AV_AudioHandle* TDAL_AVi_GetAudioDecoder(tTDAL_AV_Decoder decoder);
LOCAL tTDAL_AV_VideoHandle* TDAL_AVi_GetVideoDecoder(tTDAL_AV_Decoder decoder);
LOCAL uint8_t TDAL_AVi_ConvertDecoderIdToIndex(tTDAL_AV_Decoder decoder);
LOCAL tTDAL_AV_Error TDAL_AVi_SetStcChannel(tTDAL_AV_VideoHandle *videoHandle);
LOCAL tTDAL_AV_Error TDAL_AVi_SetAudioStcChannel(tTDAL_AV_AudioHandle *videoHandle);

/*******************************************************/
/*              Functions Definitions                  */
/*******************************************************/

/** Initializes the MODULE (not the audio and video decoders) **/
GLOBAL tTDAL_AV_Error TDAL_AV_Init(void) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;
	mTBOX_INIT_MOD_TRACE((eTDAL_AV, kTBOX_TRC_ALL));
	//mTBOX_TRACE((kTBOX_NIV_CRITICAL, “Poruka\n”));
		
	mTBOX_FCT_ENTER("TDAL_AV_Init");

	// TODO: check for errors

	// TODO: set trace level
	
    // TODO: set the initial val-ues for the audio/video structures for storing local params, 
	//		 and set the "initialized" flag

	mTBOX_TRACE((kTBOX_NIV_CRITICAL, "Initializing Video\n"));
	//------------VIDEO-------------------------------------
	/*auto err = TKEL_CreateMutex(&gVideoDecoder.mutex);
	if (err != TKEL_NO_ERR)
	{
		error = eTDAL_AV_ERROR;
	}*/
	gVideoDecoder.decoderId = eTDAL_AV_DECODER_VIDEO_1;
	gVideoDecoder.codec = eTDAL_AV_VIDEO_TYPE_UNKOWN;
	gVideoDecoder.status = eTDAL_AV_VIDEO_STATE_CLOSED;

	mTBOX_TRACE((kTBOX_NIV_CRITICAL, "Initializing Audio\n"));
	//------------AUDIO-------------------------------------
	/*err = TKEL_CreateMutex(&gAudioDecoder.mutex);
	if (err != TKEL_NO_ERR)
	{
		error = eTDAL_AV_ERROR;
	}*/
	gAudioDecoder.decoderId = eTDAL_AV_DECODER_AUDIO_1;
	gAudioDecoder.codec = eTDAL_AV_AUDIO_TYPE_UNKOWN;
	gAudioDecoder.status = eTDAL_AV_AUDIO_STATE_CLOSED;
	
	gTDAL_AV_isInitialized = true;	
	mTBOX_RETURN(error);
}

/** Deinitializes the MODULE and cleans up any leftovers **/
GLOBAL tTDAL_AV_Error TDAL_AV_Terminate(void) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;
	
	mTBOX_FCT_ENTER("TDAL_AV_Terminate");
	
	// TODO: check for errors
	 
	// TODO: stop the decoders if running and release acquired resources for local audio and video decoder structures
	//---------------------VIDEO-----------------------------------
	/*auto err = TKEL_DeleteMutex(gVideoDecoder.mutex);
	if (err != TKEL_NO_ERR)
	{
		error = eTDAL_AV_ERROR;
	}*/
	mTBOX_TRACE((kTBOX_NIV_CRITICAL, "Terminating video"));
	error = TDAL_AV_Stop(gVideoDecoder.decoderId);
	gVideoDecoder.status = eTDAL_AV_VIDEO_STATE_INVALID;

	//---------------------AUDIO-----------------------------------
	/*err = TKEL_DeleteMutex(gAudioDecoder.mutex);
	if (err != TKEL_NO_ERR)
	{
		error = eTDAL_AV_ERROR;
	}*/
	mTBOX_TRACE((kTBOX_NIV_CRITICAL, "Terminating audio"));
	error = TDAL_AV_Stop(gAudioDecoder.decoderId);
	gVideoDecoder.status = eTDAL_AV_AUDIO_STATE_INVALID;
	
	// TODO: clear the "initialized" flag
	gTDAL_AV_isInitialized = false;
	
	mTBOX_RETURN(error);
}

/** Starts decoding on the given decoder with the specified stream type/codec. **/
GLOBAL tTDAL_AV_Error TDAL_AV_Start(tTDAL_AV_Decoder decoder, tTDAL_AV_StreamType StreamType) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;

	mTBOX_FCT_ENTER("TDAL_AV_Start");
	
	// TODO: check for errors
	
	// TODO: check the status of the specified decoder
	
	// TODO: start decoding. For decoding settings, stc channel can be set on video decoder with  TDAL_AVi_SetStcChannel function and
	//       on audio decoder use NEXUS_SimpleAudioDecoder_SetStcChannel function (get decoder from TDAL_COMMON module).
	//       NEXUS PID channel can for the specified TDAL stream handle can be acuired from TDAL DMX (TDAL_DMXm_GetNexusStreamHandle).

	auto video_handle = TDAL_AVi_GetVideoDecoder(decoder);
	if (video_handle != NULL)
	{
		/* code */
		mTBOX_TRACE((kTBOX_NIV_CRITICAL, "TDAL_AV STARTING VIDEO DECODER"));
		NEXUS_SimpleVideoDecoder_GetDefaultStartSettings(&video_handle->startSettings);
		uint32_t demux_id = 0;
		auto v_pid = TDAL_DMXm_GetNexusStreamHandle(video_handle->streamHandle, &demux_id);
		video_handle->startSettings.settings.pidChannel = v_pid;
		video_handle->startSettings.settings.codec =  TDAL_AV_ConvertVideoCodecToNexus(StreamType.videoType);
		video_handle->startSettings.maxHeight = 1080;
		video_handle->startSettings.maxWidth = 1920;
		video_handle->startSettings.smoothResolutionChange = true;
		error = TDAL_AVi_SetStcChannel(video_handle);

		NEXUS_SimpleVideoDecoder_Start(video_handle->handle, &video_handle->startSettings);
		video_handle->status = eTDAL_AV_VIDEO_STATE_RUNNING;

		mTBOX_RETURN(error);
	}

	auto audio_handle = TDAL_AVi_GetAudioDecoder(decoder);
	if (audio_handle != NULL)
	{
		mTBOX_TRACE((kTBOX_NIV_CRITICAL, "TDAL_AV STARTING AUDIO DECODER"));

		NEXUS_SimpleAudioDecoder_GetDefaultStartSettings(&audio_handle->startSettings);
		uint32_t demux_id = 0;
		auto a_pid = TDAL_DMXm_GetNexusStreamHandle(audio_handle->streamHandle, &demux_id);
		auto decoderIndex = TDAL_AVi_ConvertDecoderIdToIndex(decoder);
    	auto stcHandle = TDAL_COMMONg_GetStcHandle(decoderIndex);

		audio_handle->startSettings.primary.pidChannel = a_pid;
		audio_handle->startSettings.primary.codec = TDAL_AV_ConvertAudioCodecToNexus(StreamType.audioType);

	    error = TDAL_AVi_SetAudioStcChannel(audio_handle);
	    mTBOX_TRACE((kTBOX_NIV_CRITICAL, "TDAL_AV JEBENI PRVI ERROR %d", error));

		NEXUS_SimpleAudioDecoder_Start(audio_handle->handle, &audio_handle->startSettings);
		mTBOX_TRACE((kTBOX_NIV_CRITICAL, "TDAL_AV JEBENI DRUGI ERROR %d", error));
		audio_handle->status = eTDAL_AV_AUDIO_STATE_RUNNING;


		mTBOX_RETURN(error);
	}

	// TODO: update the relevant fields of the local decoder structure (status, codec etc.)
	
	mTBOX_RETURN(error);
}

/** Stops decoding on the given decoder **/
GLOBAL tTDAL_AV_Error TDAL_AV_Stop(tTDAL_AV_Decoder decoder) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;

	mTBOX_FCT_ENTER("TDAL_AV_Stop");
	
	// TODO: check for errors
	auto video_handle = TDAL_AVi_GetVideoDecoder(decoder);
	if (video_handle != NULL)
	{
		NEXUS_SimpleVideoDecoder_Flush(video_handle->handle);

		NEXUS_SimpleVideoDecoder_Stop(video_handle->handle);

		video_handle->status = eTDAL_AV_VIDEO_STATE_STOPPED;

		mTBOX_RETURN(eTDAL_AV_NO_ERROR);
	}
	// TODO: check the status of the specified decoder

	auto audio_handle = TDAL_AVi_GetAudioDecoder(decoder);
	if (audio_handle != NULL)
	{
		NEXUS_SimpleAudioDecoder_Flush(audio_handle->handle);

		NEXUS_SimpleAudioDecoder_Stop(audio_handle->handle);

		audio_handle->status = eTDAL_AV_AUDIO_STATE_STOPPED;

		mTBOX_RETURN(eTDAL_AV_NO_ERROR);
	}
	

	// TODO: flush decoder, stop decoding and store the new decoder status in the local decoder structure
	
	error = eTDAL_AV_ERROR;
	mTBOX_RETURN(error);
}

/** Local helper function for accessing the global gAudioDecoder structure.
    Use this function for getting audio handle, do not directly use gAudioDecoder. **/
LOCAL tTDAL_AV_AudioHandle* TDAL_AVi_GetAudioDecoder(tTDAL_AV_Decoder type){

	tTDAL_AV_AudioHandle* audioHandle = NULL;

	mTBOX_FCT_ENTER("TDAL_AVi_GetAudioDecoder");
	
	// TODO: check for errors
	if (type != eTDAL_AV_DECODER_AUDIO_1)
	{
		mTBOX_RETURN(audioHandle);
	}
	audioHandle = &gAudioDecoder;	
	// TODO: Acquire initialized NEXUS audio decoder handle from the TDAL_COMMON module and store it to the local structure.
	auto idx = TDAL_AVi_ConvertDecoderIdToIndex(type);
	audioHandle->handle = TDAL_COMMONg_GetAudioDecHandle(idx);

	// TODO: Don't forget to update the status of the local audio handle structure from closed to stopped, if not already done.
	//audioHandle->status = eTDAL_AV_AUDIO_STATE_STOPPED;

	mTBOX_RETURN(audioHandle);
}

/** Local helper function for accessing the global gVideoDecoder structure.
    Use this function for getting video handle, do not directly use gVideoDecoder. **/
LOCAL tTDAL_AV_VideoHandle* TDAL_AVi_GetVideoDecoder(tTDAL_AV_Decoder type) {

	tTDAL_AV_VideoHandle* videoHandle = NULL;
	
	mTBOX_FCT_ENTER("TDAL_AVi_GetVideoDecoder");

	// TODO: check for errors
	if (type != eTDAL_AV_DECODER_VIDEO_1)
	{
		mTBOX_RETURN(videoHandle);
	}
	videoHandle = &gVideoDecoder;
	// TODO: Acquire initialized NEXUS video decoder handle from the TDAL_COMMON module and store it to the local structure.
	auto idx = TDAL_AVi_ConvertDecoderIdToIndex(type);
	videoHandle->handle = TDAL_COMMONg_GetVideoDecHandle(idx);

	// TODO: Don't forget to update the status of the local video handle structure from closed to stopped, if not already done.
	//videoHandle->status = eTDAL_AV_VIDEO_STATE_STOPPED;
	
	mTBOX_RETURN(videoHandle);
}

/** Sets stream for the specified decoder **/
GLOBAL tTDAL_AV_Error TDAL_AV_InputStreamSet(tTDAL_AV_Decoder decoder, uint32_t streamHandle) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;

	mTBOX_FCT_ENTER("TDAL_AV_InputStreamSet");
	
	// TODO: check for errors
	
	// TODO: store the stream handle to the appropirate local decoder structure (audio/video)

	auto video_handle = TDAL_AVi_GetVideoDecoder(decoder);
	if (video_handle != NULL)
	{
		video_handle->streamHandle = streamHandle;
		mTBOX_RETURN(error);
	}

	auto audio_handle = TDAL_AVi_GetAudioDecoder(decoder);
	if (audio_handle != NULL)
	{
		audio_handle->streamHandle = streamHandle;
		mTBOX_RETURN(error);
	}

	error = eTDAL_AV_ERROR;	
	mTBOX_RETURN(error);
}

/**  Converts video codec type from TDAL to NEXUS **/
GLOBAL NEXUS_VideoCodec TDAL_AV_ConvertVideoCodecToNexus(tTDAL_AV_VideoType type){

	if(type == eTDAL_AV_VIDEO_TYPE_H264) return NEXUS_VideoCodec_eH264;
	if(type == eTDAL_AV_VIDEO_TYPE_MPEG1) return NEXUS_VideoCodec_eMpeg1;
	if(type == eTDAL_AV_VIDEO_TYPE_MPEG2) return NEXUS_VideoCodec_eMpeg2;
#ifdef PLATFORM_HAS_HEVC
	if(type == eTDAL_AV_VIDEO_TYPE_HEVC) return NEXUS_VideoCodec_eH265;
#endif

	return NEXUS_VideoCodec_eUnknown;
}

/**  Converts video codec type from NEXUS to TDAL **/
GLOBAL tTDAL_AV_VideoType TDAL_AV_ConvertVideoCodecToTdal(NEXUS_VideoCodec type){

	if(type == NEXUS_VideoCodec_eH264) return eTDAL_AV_VIDEO_TYPE_H264;
	if(type == NEXUS_VideoCodec_eMpeg1) return eTDAL_AV_VIDEO_TYPE_MPEG1;
	if(type == NEXUS_VideoCodec_eMpeg2) return eTDAL_AV_VIDEO_TYPE_MPEG2;
#ifdef PLATFORM_HAS_HEVC
	if(type == NEXUS_VideoCodec_eH265) return eTDAL_AV_VIDEO_TYPE_HEVC;
#endif

	return eTDAL_AV_VIDEO_TYPE_MPEG2;
}

/**  Converts audio codec type from TDAL to NEXUS **/
GLOBAL NEXUS_AudioCodec TDAL_AV_ConvertAudioCodecToNexus(tTDAL_AV_AudioType type){

	if(type == eTDAL_AV_AUDIO_TYPE_MPEG) return NEXUS_AudioCodec_eMpeg;
	if(type == eTDAL_AV_AUDIO_TYPE_HE_AAC) return NEXUS_AudioCodec_eAacPlusLoas;
	if(type == eTDAL_AV_AUDIO_TYPE_MPEG_AAC) return NEXUS_AudioCodec_eAac;

#if PLATFORM_HAS_AC3
	if(type == eTDAL_AV_AUDIO_TYPE_AC3) return NEXUS_AudioCodec_eAc3;
	if(type == eTDAL_AV_AUDIO_TYPE_EAC3) return NEXUS_AudioCodec_eAc3Plus;
#endif

	return NEXUS_AudioCodec_eUnknown;
}

/**  Converts audio codec type from NEXUS to TDAL **/
GLOBAL tTDAL_AV_AudioType TDAL_AV_ConvertAudioCodecToTdal(NEXUS_AudioCodec type) {
	if(type == NEXUS_AudioCodec_eMpeg) return eTDAL_AV_AUDIO_TYPE_MPEG;
	if(type == NEXUS_AudioCodec_eAacPlusLoas) return eTDAL_AV_AUDIO_TYPE_HE_AAC;
	if(type == NEXUS_AudioCodec_eAac) return eTDAL_AV_AUDIO_TYPE_MPEG_AAC;
#if PLATFORM_HAS_AC3
	if(type == NEXUS_AudioCodec_eAc3) return eTDAL_AV_AUDIO_TYPE_AC3;
	if(type == NEXUS_AudioCodec_eAc3Plus) return eTDAL_AV_AUDIO_TYPE_EAC3;
#endif

	return eTDAL_AV_AUDIO_TYPE_MPEG;
}

/** Returns the module capabilities (i.e. available audio and video decoders) **/
GLOBAL tTDAL_AV_Error TDAL_AV_CapabilityGet(tTDAL_AV_Capability *pstCapability /* [out] */) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;
	
	mTBOX_FCT_ENTER("TDAL_AV_CapabilityGet");
	
	// TODO: check for errors
	if (pstCapability == NULL)
	{
		mTBOX_RETURN(eTDAL_AV_BAD_PARAMETER_ERROR);
	}

	// TODO: fill in the supported decoders for BCM platform
    pstCapability->decoderSupported = eTDAL_AV_DECODER_VIDEO_1 | eTDAL_AV_DECODER_AUDIO_1;
	
	mTBOX_RETURN(error);
}

/** Returns the video capabilities **/
GLOBAL tTDAL_AV_Error TDAL_AV_VideoCapabilityGet(tTDAL_AV_Decoder decoder,tTDAL_AV_VideoCapability *pstCapability) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;
	
	mTBOX_FCT_ENTER("TDAL_AV_VideoCapabilityGet");

	// TODO: check for errors
	if (pstCapability == NULL)
	{
		mTBOX_RETURN(eTDAL_AV_BAD_PARAMETER_ERROR);
	}

	auto handle = TDAL_AVi_GetVideoDecoder(decoder);
	if (handle == NULL)
	{
		mTBOX_RETURN(eTDAL_AV_BAD_PARAMETER_ERROR);
	}
	
	// TODO: fill in the video capabilities. Freeze, picture capture and speed are not supported. 
	// Max speed forward/backward speed is 1. Supporetd picture types are MPEG YUV420 and RGB.
	// Supported video types are MPEG_1, MPEG_2, H264 and HEVC .

	pstCapability->isFreezeSupported = false;
    pstCapability->isPictureCaptureSupported = false;
    pstCapability->isSpeedSupported = false;
    pstCapability->pictureTypeSupported = eTDAL_AV_PICTURE_MPEG | eTDAL_AV_PICTURE_YUV_420 | eTDAL_AV_PICTURE_RGB;
    pstCapability->videoTypeSupported = eTDAL_AV_VIDEO_TYPE_H264 | eTDAL_AV_VIDEO_TYPE_MPEG2 | eTDAL_AV_VIDEO_TYPE_MPEG1 | eTDAL_AV_VIDEO_TYPE_HEVC;
    pstCapability->speedBackwardMax = 1;
    pstCapability->speedForwardMax = 1;
	
	mTBOX_RETURN(error);
}

/** Returns the audio capabilities **/
GLOBAL tTDAL_AV_Error TDAL_AV_AudioCapabilityGet(tTDAL_AV_Decoder decoder, tTDAL_AV_AudioCapability *pstCapability /*[out]*/) {

	tTDAL_AV_Error error = eTDAL_AV_NO_ERROR;
	
	mTBOX_FCT_ENTER("TDAL_AV_AudioCapabilityGet");

	// TODO: check for errors
	if (pstCapability == NULL)
	{
		mTBOX_RETURN(eTDAL_AV_BAD_PARAMETER_ERROR);
	}

	auto handle = TDAL_AVi_GetAudioDecoder(decoder);
	if (handle == NULL)
	{
		mTBOX_RETURN(eTDAL_AV_BAD_PARAMETER_ERROR);
	}

	// TODO: fill in the audio types. Supported are: MPEG, MPEG AAC, AC3, HE AAC and EAC3. 

	pstCapability->sampleTypeSupported = 0;
    pstCapability->audioTypeSupported = 
		eTDAL_AV_AUDIO_TYPE_AC3 |
		eTDAL_AV_AUDIO_TYPE_MPEG |
		eTDAL_AV_AUDIO_TYPE_MPEG_AAC |
		eTDAL_AV_AUDIO_TYPE_HE_AAC |
		eTDAL_AV_AUDIO_TYPE_EAC3;
    pstCapability->isBeepSupported = false;
    pstCapability->isSpeedSupported = false;
    pstCapability->speedBackwardMax = 1;
    pstCapability->speedForwardMax = 1;

	mTBOX_RETURN(error);
}

/*******************************************************/
/*              Should not be implemented			   */
/*******************************************************/
GLOBAL tTDAL_AV_Error TDAL_AVm_GetPtsStc(tTDAL_AV_Decoder decoder, uint32_t* pts, uint32_t* stc, uint32_t* queueDepth) {

	mTBOX_FCT_ENTER("TDAL_AVm_GetPtsStc");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

GLOBAL tTDAL_AV_Error TDAL_AV_EventSubscribe(tTDAL_AV_Decoder decoder,
		tTDAL_AV_Event event, tTDAL_AV_CallbackProc_t notifyFunction) {

	mTBOX_FCT_ENTER("TDAL_AV_EventSubscribe");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

GLOBAL tTDAL_AV_Error TDAL_AV_AudioStereoModeSet(tTDAL_AV_Decoder decoder,
		tTDAL_AV_AudioStereoMode mode) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioStereoModeSet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

GLOBAL void TDAL_AV_Flush(tTDAL_AV_Decoder decoder) {

	mTBOX_FCT_ENTER("TDAL_AV_Flush");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN;
}

GLOBAL tTDAL_AV_Error TDAL_AV_SpeedSet(tTDAL_AV_Decoder decoder,int32_t speed){

	mTBOX_FCT_ENTER("TDAL_AV_SpeedSet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_AudioDestinationSet(tTDAL_AV_Decoder eDecoder,
		uint32_t SpeakerHandle) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioDestinationSet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

GLOBAL void TDAL_AVm_GetCurrentRowPts(uint32_t decoder, unsigned long *pts) {

	mTBOX_FCT_ENTER("TDAL_AVm_GetCurrentRowPts");

	mTBOX_RETURN;
}

tTDAL_AV_Error TDAL_AV_Scale(tTDAL_DISP_LayerWindow *pstOutputWindow) {
	mTBOX_FCT_ENTER("TDAL_AV_Scale");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_AudioSampleStart(tTDAL_AV_Decoder decoder,
		tTDAL_AV_SampleType sampleType, uint8_t* pData, uint32_t size,
		uint16_t nbTimes) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioSampleStart");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_AudioSampleStop(tTDAL_AV_Decoder decoder) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioSampleStop");
	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_AudioBeepPlay(tTDAL_AV_Decoder decoder,
		uint16_t frequency, uint16_t duration) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioBeepPlay");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_AudioStereoModeGet(tTDAL_AV_Decoder decoder,
		tTDAL_AV_AudioStereoMode *pstMode) {
	mTBOX_FCT_ENTER("TDAL_AV_AudioStereoModeGet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoDestinationSet(tTDAL_AV_Decoder decoder,
		uint32_t layerHandle) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoDestinationSet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoFreeze(tTDAL_AV_Decoder decoder) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoFreeze");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoPictureStart(tTDAL_AV_Decoder decoder,
		tTDAL_AV_PictureType pictureType, uint8_t* pData, uint32_t size,
		tTDAL_AV_VideoPictureParams *pstPictureParams) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoPictureStart");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoPictureStop(tTDAL_AV_Decoder decoder) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoPictureStop");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoPictureCaptureGet(tTDAL_AV_Decoder decoder,
		tTDAL_AV_PictureType pictureType, uint8_t** ppData, uint32_t *pSize) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoPictureCaptureGet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_VideoPictureCaptureRelease(tTDAL_AV_Decoder decoder,
		tTDAL_AV_PictureType pictureType, uint8_t* pData, uint32_t size) {
	mTBOX_FCT_ENTER("TDAL_AV_VideoPictureCaptureRelease");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_EventUnsubscribe(tTDAL_AV_Decoder decoder,
		tTDAL_AV_Event event) {
	mTBOX_FCT_ENTER("TDAL_AV_EventUnsubscribe");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

tTDAL_AV_Error TDAL_AV_SynchroModeSet(tTDAL_AV_Decoder videoDecoder,
		tTDAL_AV_Decoder audioDecoder, tTDAL_AV_SynchroMode mode) {
	mTBOX_FCT_ENTER("TDAL_AV_SynchroModeSet");

	mTBOX_TRACE((kTBOX_NIV_1,"%s : Is not implemented! \n", __FUNCTION__));

	mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}


LOCAL uint8_t TDAL_AVi_ConvertDecoderIdToIndex(tTDAL_AV_Decoder decoder)
{
    uint8_t index = 0;

    mTBOX_FCT_ENTER("TDAL_AVi_ConvertDecoderIdToIndex");

    if (decoder >= eTDAL_AV_DECODER_VIDEO_1 && decoder < eTDAL_AV_DECODER_VIDEO_8){
        while (decoder>>index > eTDAL_AV_DECODER_VIDEO_1){
            index++;
        }
    } else if (decoder >= eTDAL_AV_DECODER_AUDIO_1 && decoder < eTDAL_AV_DECODER_AUDIO_8) {
        while (decoder>>index > eTDAL_AV_DECODER_AUDIO_1){
            index++;
        }
    } else {
        mTBOX_RETURN(-1);
    }

    mTBOX_TRACE((kTBOX_NIV_1, "%s: returning index %d\n", __FUNCTION__, index));
    mTBOX_RETURN(index);
}

LOCAL tTDAL_AV_Error TDAL_AVi_SetStcChannel(tTDAL_AV_VideoHandle *videoHandle)
{
    NEXUS_SimpleStcChannelHandle stcHandle;
    NEXUS_SimpleStcChannelSettings stcSettings;
    tTDAL_AV_Decoder audioDecoder;
    tTDAL_AV_AudioHandle* audioHandle;
    uint8_t decoderIndex;
    NEXUS_Error rc;

    mTBOX_FCT_ENTER("TDAL_AVi_SetStcChannel");

    decoderIndex = TDAL_AVi_ConvertDecoderIdToIndex(videoHandle->decoderId);
    stcHandle = TDAL_COMMONg_GetStcHandle(decoderIndex);

    if (stcHandle == NULL){
        mTBOX_RETURN(eTDAL_AV_ERROR);
    }

    NEXUS_SimpleStcChannel_GetSettings(stcHandle, &stcSettings);
    stcSettings.mode = NEXUS_StcChannelMode_eAuto;
    stcSettings.modeSettings.Auto.transportType = NEXUS_TransportType_eTs;
    stcSettings.modeSettings.Auto.behavior = NEXUS_StcChannelAutoModeBehavior_eFirstAvailable;
    NEXUS_SimpleStcChannel_SetSettings(stcHandle, &stcSettings);

    rc = NEXUS_SimpleVideoDecoder_SetStcChannel(videoHandle->handle, stcHandle);
    if (rc) {
        mTBOX_TRACE((kTBOX_NIV_CRITICAL, "%s: Error setting stc channel on video decoder, rc = %d\n", __FUNCTION__, rc));
        mTBOX_RETURN(eTDAL_AV_ERROR);
    }

    mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

LOCAL tTDAL_AV_Error TDAL_AVi_SetAudioStcChannel(tTDAL_AV_AudioHandle *videoHandle)
{
    NEXUS_SimpleStcChannelHandle stcHandle;
    NEXUS_SimpleStcChannelSettings stcSettings;
    tTDAL_AV_Decoder audioDecoder;
    tTDAL_AV_AudioHandle* audioHandle;
    uint8_t decoderIndex;
    NEXUS_Error rc;

    mTBOX_FCT_ENTER("TDAL_AVi_SetAudioStcChannel");

    decoderIndex = TDAL_AVi_ConvertDecoderIdToIndex(videoHandle->decoderId);
    stcHandle = TDAL_COMMONg_GetStcHandle(decoderIndex);

    if (stcHandle == NULL){
        mTBOX_RETURN(eTDAL_AV_ERROR);
    }

    NEXUS_SimpleStcChannel_GetSettings(stcHandle, &stcSettings);
    stcSettings.mode = NEXUS_StcChannelMode_eAuto;
    stcSettings.modeSettings.Auto.transportType = NEXUS_TransportType_eTs;
    stcSettings.modeSettings.Auto.behavior = NEXUS_StcChannelAutoModeBehavior_eFirstAvailable;
    NEXUS_SimpleStcChannel_SetSettings(stcHandle, &stcSettings);

    rc = NEXUS_SimpleAudioDecoder_SetStcChannel(videoHandle->handle, stcHandle);
    if (rc) {
        mTBOX_TRACE((kTBOX_NIV_CRITICAL, "%s: Error setting stc channel on audio decoder, rc = %d\n", __FUNCTION__, rc));
        mTBOX_RETURN(eTDAL_AV_ERROR);
    }

    mTBOX_RETURN(eTDAL_AV_NO_ERROR);
}

bool TDAL_AVm_EnableAudio( void )
{
	return true;
}
