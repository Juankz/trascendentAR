package org.glud.ar;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class main extends ApplicationAdapter {
	final static String TAG = "AR Application";
	SpriteBatch batch;
	Texture img;
	ARToolKitManager arToolKitManager;
	Music musica;
	float volumen;
	float delta;
	int marcadorId;
	Vector2 posicion;

	AR_Camera camera;
	ModelBatch batch_3d;
	Model model;
	ModelInstance modelInstance;
	Environment environment;
	Array<ModelInstance> instanceArray;
	AssetManager manager;
	String model_name = "palmera.g3dj";
	boolean loading = true;
	Matrix4 matriz_transformacion = new Matrix4();
	Stage stage;
	Label label;
	Timer timer;

	Vector3 object_position = new Vector3();
	Vector3 object_scale = new Vector3();
	Quaternion object_rotation = new Quaternion();

	StringBuilder stringBuilder;

	boolean update_label= false;

	public main(ARToolKitManager arToolKitManager){
		this.arToolKitManager = arToolKitManager;
	}
	
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		batch = new SpriteBatch();
		img = new Texture("musica.png");
		musica = Gdx.audio.newMusic(Gdx.files.internal("musica.ogg"));
		musica.setLooping(true);
		posicion = new Vector2(Gdx.graphics.getWidth()*0.5f - img.getWidth()*0.5f ,
				Gdx.graphics.getHeight()*0.5f - img.getHeight()*0.5f);

		//3D

		camera = new AR_Camera(67,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		camera.position.set(10f,10f,10f);
		camera.lookAt(0,0,0);
		camera.near = 1f;
		camera.far = 300f;
		camera.update();
		manager = new AssetManager();
		manager.load(model_name,Model.class);

		instanceArray = new Array<ModelInstance>();
		batch_3d = new ModelBatch();

		//Adding lights
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		//UI
		stage = new Stage(new StretchViewport(640,360),batch);
		Label.LabelStyle labelStyle = new Label.LabelStyle(new BitmapFont(), Color.YELLOW);
		label = new Label("",labelStyle);
		label.setPosition(0,200);
		label.setWrap(true);
		label.setText(matriz_transformacion.toString());
		stage.addActor(label);

		stringBuilder = new StringBuilder();

		timer = new Timer();
		timer.scheduleTask(new Timer.Task() {
			@Override
			public void run() {
				update_label = true;
			}
		},0,0.5f,10000000);
		//cargar macardor
//		marcadorId = arToolKitManager.cargarMarcador("single;Data/hiro.patt;80");
//		Gdx.app.debug(TAG,"Marcador ID = "+marcadorId);
//		if(marcadorId < 0){
//			Gdx.app.error(TAG,"marcador no cargado");
//		}else {
//			Gdx.app.debug(TAG,"marcador cargado");
//		}
	}

	@Override
	public void render () {
		delta = Gdx.graphics.getDeltaTime();
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		if(arToolKitManager.marcadorVisible(marcadorId)){
			matriz_transformacion.set(arToolKitManager.getTransformMatrix(marcadorId));
			print_info();
			if(!musica.isPlaying()) {
				musica.play();
			}
			if(volumen < 0.99) {
				volumen += 0.5*delta;
				musica.setVolume(volumen);
			}
			batch.begin();
			batch.draw(img,posicion.x,posicion.y);
			batch.end();
			//Render
			camera.projection.set(arToolKitManager.getProjectionMatrix());
			camera.update();
			for(ModelInstance instance : instanceArray){
				instance.transform.set(matriz_transformacion);
				instance.calculateTransforms();
			}
			batch_3d.begin(camera);
			batch_3d.render(instanceArray,environment);
			batch_3d.end();
		}else{
			if(musica.isPlaying()) {
				volumen -= 0.5*delta;
				musica.setVolume(volumen);
				if(volumen < 0.001) {
					musica.pause();
				}
			}
		}
		if(loading && manager.update()){
			done_loading();
		}
		stage.act();
		stage.draw();
	}

	private void done_loading(){
		model = manager.get(model_name);
		modelInstance = new ModelInstance(model);
		instanceArray.add(modelInstance);
		loading=false;
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}

	private void print_info(){
		if(!update_label)return;
		update_label = false;
		//label.setText(matriz_transformacion.toString());

		matriz_transformacion.getTranslation(object_position);
		matriz_transformacion.getScale(object_scale);
		matriz_transformacion.getRotation(object_rotation);

		stringBuilder.setLength(0);
		stringBuilder.append("DETALLES DEL MARCADOR:");
		stringBuilder.append("\nPosicion: "+object_position);
		stringBuilder.append("\nEscala: "+object_scale);
		stringBuilder.append("\nRotacion: "+object_rotation);

		label.setText(stringBuilder);

		/*
		matriz_transformacion.set(arToolKitManager.getTransformMatrix(marcadorId));
		Gdx.app.debug(TAG,"=========================================");
		Gdx.app.debug(TAG,"\n"+matriz_transformacion.toString());
		*/
	}

}
