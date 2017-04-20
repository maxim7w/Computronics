package pl.asie.computronics.audio.tts;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.server.Mary;
import marytts.util.data.audio.AudioPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.computronics.reference.Mods;
import pl.asie.computronics.tile.TileSpeechBox;
import pl.asie.lib.util.WorldUtils;

import javax.sound.sampled.AudioInputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Vexatos
 */
public class TextToSpeech {

	public MaryInterface marytts;

	private ExecutorService ttsThreads;
	public final ArrayList<Future<Result>> processes = new ArrayList<Future<Result>>();
	public static final Logger log = LogManager.getLogger(Mods.Computronics + "-text-to-speech");

	public TextToSpeech() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void say(String text, int dimID, BlockPos pos) {
		Future<Result> fut = ttsThreads.submit(new SynthesizeTask(text, dimID, pos.getX(), pos.getY(), pos.getZ()));
		processes.add(fut);
	}

	@SubscribeEvent
	public void onTick(ServerTickEvent e) {
		if(e.phase == Phase.START) {
			ArrayList<Integer> toRemove = new ArrayList<Integer>();
			for(int i = 0; i < processes.size(); i++) {
				Future<Result> process = processes.get(i);
				if(process.isDone()) {
					try {
						Result result = process.get();
						if(result != null) {
							TileEntity tile = WorldUtils.getTileEntityServer(result.dimID, result.x, result.y, result.z);
							if(tile instanceof TileSpeechBox) {
								((TileSpeechBox) tile).startTalking(result.data);
							}
						}
					} catch(Throwable t) {
						log.error("Error while playing text to speech", t);
					}
					toRemove.add(i);
				}
			}
			for(int i : toRemove) {
				processes.remove(i);
			}
		}
	}

	public static class Result {

		private final byte[] data;
		private final int dimID;
		private final int x;
		private final int y;
		private final int z;

		public Result(byte[] data, int dimID, int x, int y, int z) {
			this.data = data;
			this.dimID = dimID;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	public boolean preInit() {
		try {
			marytts = new LocalMaryInterface();
			//Set<String> voices = marytts.getAvailableVoices();
			marytts.setStreamingAudio(true);
			String voice = marytts.getVoice();
			if(voice == null) {
				Set<Locale> availableLocales = marytts.getAvailableLocales();
				for(Locale locale : availableLocales) {
					marytts.setLocale(locale);
					voice = marytts.getVoice();
					if(voice != null) {
						break;
					}
				}
				if(voice == null) {
					log.error("No voice found for available locales, please install a matching voice file. Available locales: " + StringUtils.join(availableLocales, ", "));
					if(Mary.currentState() == 2) {
						Mary.shutdown();
					}
					marytts = null;
					return false;
				}
			}
			//marytts.setLocale(Locale.US);
			//marytts.setVoice(voices.iterator().next());
			marytts.setOutputType("AUDIO");
			ttsThreads = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setPriority(Thread.MIN_PRIORITY).build());
			log.info("Text To Speech successfully initialized with language " + marytts.getLocale() + " and voice " + voice + ".");
			return true;
		} catch(Exception e) {
			log.error("Text To Speech initialization failed, you will not be able to hear anything", e);
			if(Mary.currentState() == 2) {
				Mary.shutdown();
			}
			marytts = null;
			return false;
		}
	}

	public static void main(String[] args) {
		try {
			MaryInterface marytts = new LocalMaryInterface();
			Set<String> voices = marytts.getAvailableVoices();
			marytts.setVoice(voices.iterator().next());
			marytts.setLocale(Locale.US);
			AudioInputStream audio = marytts.generateAudio("I am a pastry fork.");
			AudioPlayer player = new AudioPlayer(audio);
			ExecutorService service = Executors.newCachedThreadPool();
			service.submit(player).get();
		} catch(MaryConfigurationException e) {
			e.printStackTrace();
			e.getCause().printStackTrace();
			e.getCause().getCause().printStackTrace();
		} catch(SynthesisException e) {
			e.printStackTrace();
		} catch(InterruptedException e) {
			e.printStackTrace();
		} catch(ExecutionException e) {
			e.printStackTrace();
		}
	}

	/*static{
		ClassLoader classLoader = TextToSpeech.class.getClassLoader();
		System.out.println("Classloader: " + classLoader.toString());
	}*/
}