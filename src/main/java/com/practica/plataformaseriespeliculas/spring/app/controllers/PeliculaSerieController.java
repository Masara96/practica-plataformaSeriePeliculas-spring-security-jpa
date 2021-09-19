package com.practica.plataformaseriespeliculas.spring.app.controllers;

import java.io.IOException;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.practica.plataformaseriespeliculas.spring.app.models.entity.PeliculaSerie;
import com.practica.plataformaseriespeliculas.spring.app.models.entity.Personaje;
import com.practica.plataformaseriespeliculas.spring.app.service.IService;
import com.practica.plataformaseriespeliculas.spring.app.service.IUpdateService;
import com.practica.plataformaseriespeliculas.spring.app.util.paginator.PageRender;

@Controller
@SessionAttributes("trabajo")
public class PeliculaSerieController {

	private static final Logger log = LoggerFactory.getLogger(PeliculaSerieController.class);

	@Autowired
	private IService serviceDao;

	@Autowired
	private IUpdateService updateService;
   

	
	@GetMapping(value = "/peliculas")
	public String listarPeliculas(@RequestParam(name = "page",defaultValue = "0") int page,Model model) {
        
		Pageable pageRequest = PageRequest.of(page, 4);
		
		Page<PeliculaSerie> peliculas = serviceDao.findPeliculaAll(pageRequest);
       
		PageRender<PeliculaSerie> pageRender = new PageRender<PeliculaSerie>("peliculas", peliculas);
		
		model.addAttribute("title", "Listado de Peliculas");
		model.addAttribute("peliculas", peliculas);
		model.addAttribute("page",pageRender);
		return "pelicula-serie/listar";
	}

	@GetMapping(value = "/series")
	public String listarSerie(@RequestParam(name = "page",defaultValue = "0") int page,Model model) {

        Pageable pageRequest = PageRequest.of(page, 4);
		
		Page<PeliculaSerie> series = serviceDao.findSerieAll(pageRequest);
       
		PageRender<PeliculaSerie> pageRender = new PageRender<PeliculaSerie>("series", series);

		model.addAttribute("title", "Listado de Serie");
		model.addAttribute("series", series);
		model.addAttribute("page",pageRender);

		return "pelicula-serie/listar";
	}
	
	@GetMapping(value = "/cargar-personaje/{term}", produces = { "application/json" })
	public @ResponseBody List<Personaje> cargarProductos(@PathVariable String term) {
		return serviceDao.findByNombre(term);
	}

	@GetMapping("/pelicula/form")
	public String crearPelicula(Model model) {

		PeliculaSerie pelicula = new PeliculaSerie("PELICULA");
		

		log.info("Fecha : " + pelicula.getCreateAt());
		log.info("Nombre pelicula : " + pelicula.getTitulo());
		log.info("Tipo : " + pelicula.getTipo());

		model.addAttribute("trabajo", pelicula);
		model.addAttribute("title", "Crear Pelicula");
		model.addAttribute("boton", "Crear Pelicula");

		return "pelicula-serie/form";
	}

	@GetMapping("/serie/form")
	public String crearSerie(Model model) {

		PeliculaSerie serie = new PeliculaSerie("SERIE");

		log.info("Fecha : " + serie.getCreateAt());
		log.info("Nombre pelicula : " + serie.getTitulo());
		log.info("Tipo : " + serie.getTipo());

		model.addAttribute("trabajo", serie);
		model.addAttribute("title", "Crear Serie");
		model.addAttribute("boton", "Crear Serie");

		return "pelicula-serie/form";
	}

	@PostMapping("/pelicula-serie/form")
	public String guardar(@Valid @ModelAttribute("trabajo") PeliculaSerie trabajo, BindingResult result, Model model,
			@RequestParam(name = "item_id[]", required = false) Long[] itemId,
			RedirectAttributes flash, @RequestParam("file") MultipartFile imagen, SessionStatus status) {



		if (result.hasErrors()) {
			if (trabajo.getTipo().equalsIgnoreCase("PELICULA")) {
				model.addAttribute("title", "Crear Pelicula");
				model.addAttribute("boton", "Crear Pelicula");
			} else {
				model.addAttribute("title", "Crear Serie");
				model.addAttribute("boton", "Crear Serie");
			}
			return "pelicula-serie/form";
		}
		
		if(itemId == null || itemId.length == 0 ) {
			
			if (trabajo.getTipo().equalsIgnoreCase("PELICULA")) {
				model.addAttribute("title", "Crear Pelicula");
				model.addAttribute("boton", "Crear Pelicula");
			} else {
				model.addAttribute("title", "Crear Serie");
				model.addAttribute("boton", "Crear Serie");
			}
			model.addAttribute("error","ERROR: La pelicula No puede no tener personajes!" );
			return "pelicula-serie/form";
		}

		if (!imagen.isEmpty()) {

			if (trabajo.getId() != null && trabajo.getId() > 0 && trabajo.getImagen() != null) {
				updateService.delete(trabajo.getImagen());
			}

			String uniqueFilename = null;

			try {
				uniqueFilename = updateService.copy(imagen);
			} catch (IOException e) {
				log.error(e.getLocalizedMessage());
			}

			flash.addFlashAttribute("info", "Se ha subido la imagen con exito '" + uniqueFilename + "' correctamente.");
			trabajo.setImagen(uniqueFilename);
		}
		
		log.info("Cantidad de items = " + itemId.length);
         
		for (int i = 0; i < itemId.length; i++) {
		    Personaje personaje = serviceDao.findPersonajesById(itemId[i]);
			trabajo.setPersonaje(personaje);
		}
           
		log.info("Cantidad de personajes dentro de Pelicula o Serie : " + trabajo.getPersonajes().size());
		serviceDao.savePeliculaSerie(trabajo);
		
		flash.addFlashAttribute("success", "Se ha guardado el registro correctamente!");
		status.setComplete();
	
		if(trabajo.getTipo().equalsIgnoreCase("PELICULA")) {
		  return "redirect:/peliculas";
		}else return "redirect:/series";
		
	}

	@RequestMapping(value = "/pelicula-serie/editar/{id}", method = RequestMethod.GET)
	public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {

		PeliculaSerie peliculaSerie = serviceDao.findPeliculaSerieById(id);

		if (peliculaSerie == null) {
			  flash.addFlashAttribute("error", "ERROR: No se ha encontrado en la Base de Datos");
			  return "redirect:/";
		}
		
		if(peliculaSerie.getTipo().equalsIgnoreCase("PELICULA") ) { 
		model.addAttribute("title", "Modificar Pelicula");
		} else model.addAttribute("title", "Modificar Serie");
		
		model.addAttribute("boton", "Modificar");
		model.addAttribute("trabajo", peliculaSerie);
		return "pelicula-serie/form";
	}

	@GetMapping(value = "/pelicula-serie/ver/{id}")
	public String ver(@PathVariable Long id, Model model, RedirectAttributes flash) {

		PeliculaSerie peliculaSerie = serviceDao.findPeliculaSerieById(id);

		if (peliculaSerie == null) {
			  flash.addFlashAttribute("error", "ERROR: No se ha encontrado en la Base de Datos");
			  return "redirect:/";
		}

		if(peliculaSerie.getTipo().equalsIgnoreCase("PELICULA")) { 
			model.addAttribute("title", "Detalle Pelicula");
			} else model.addAttribute("title", "Detalle Serie");
		
		model.addAttribute("trabajo", peliculaSerie);
		return "pelicula-serie/ver";
	}

	@GetMapping("/pelicula-serie/eliminar/{id}")
	public String eliminar(@PathVariable Long id, RedirectAttributes flash) {

		PeliculaSerie peliculaSerie = serviceDao.findPeliculaSerieById(id);

		if (peliculaSerie == null) {
			flash.addFlashAttribute("error", "No se encuenta el registro en la BD");
			return "redirect:/index";
		}

		serviceDao.deletePeliculaSerie(id);

		if (peliculaSerie.getImagen() != null) {
			if (updateService.delete(peliculaSerie.getImagen())) {
				log.info("Se ha eliminado la imagen '" + peliculaSerie.getImagen() + "' correctamente");
			}
		}
        
		if(peliculaSerie.getTipo().equalsIgnoreCase("PELICULA")) {
		flash.addFlashAttribute("success", "Se ha eliminado la pelicula correctamente");
		return "redirect:/peliculas";
		}else {
			flash.addFlashAttribute("success", "Se ha eliminado la serie correctamente");	
			return "redirect:/series";
		}
		
	}
	

	
	
//	Map< String, String> errores = new HashMap<>();
//	result.getFieldErrors().forEach(err-> {
//		errores.put(err.getField(), "El campo".concat(err.getField().concat(" ").concat(err.getDefaultMessage())));
//	});

//	model.addAttribute("errores", errores);		

}
