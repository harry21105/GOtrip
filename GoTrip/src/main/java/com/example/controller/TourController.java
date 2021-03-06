package com.example.controller;
import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.example.dao.MemberDAO;
import com.example.dao.TourDAO;
import com.example.entity.Account;
import com.example.entity.Tour;
import com.example.storage.StorageService;

@Controller
@RequestMapping(value = "/user")
public class TourController {
	
	@Autowired
	TourDAO dao;
	
	@Autowired
	MemberDAO memberDao;
	
	private final StorageService storageService;

	@Autowired
	public TourController(StorageService storageService) {
		this.storageService = storageService;
	}

	@RequestMapping(value = "/tourCreate", method = RequestMethod.GET)
	public ModelAndView openFormCreate(Principal principal) {
		ModelAndView model = new ModelAndView("Tour/tourCreate");
		Tour tour = new Tour();
		model.addObject("tour", tour);
		model.addObject("curName", getUserName());
		
		return model;
	}

	@RequestMapping(value = "/tourCreate", method = RequestMethod.POST)
	public ModelAndView processFormCreate(@Valid @ModelAttribute Tour list, BindingResult bindingResult, Principal principal) throws IllegalStateException, IOException {
		ModelAndView model = new ModelAndView("redirect:/user/tourRetrieveAll");
			
		if(bindingResult.hasErrors()){
			model = new ModelAndView("Tour/tourCreate");
			
			return model;
		}
		if(list.getPhotoFile().isEmpty()) 
			list.setPhotoByParam("defaultImg" + ((int)(Math.random()*3+1)) + ".jpg");	
		else {
			MultipartFile pic = list.getPhotoFile();
			storageService.store(pic);
			list.setPhoto();
		}
		// copy file name to the field photo
		Account account = memberDao.findOne(principal.getName());
		list.setAccountTour(account);
		dao.save(list);

		return model;
	}

	@RequestMapping(value = { "/tourRetrieveAll" }, method = RequestMethod.GET)
	public ModelAndView retrieveTour(Principal principal) throws SQLException {	
		ModelAndView model = new ModelAndView("Tour/tourList");
		Account account = memberDao.findOne(principal.getName());
		Iterable<Tour> list = account.getTours();
		model.addObject("tourlists", list);
		model.addObject("curName", getUserName());
		
		return model;
	}

	@RequestMapping(value = "/tourUpdate", method = RequestMethod.GET)
	public ModelAndView processFormUpdate(@RequestParam(value = "id", required = false, defaultValue = "1") 
					Long id) throws SQLException {
		ModelAndView model = new ModelAndView("Tour/tourUpdate");
		Tour list = dao.findOne(id);
		model.addObject("tour", list);
		model.addObject("curName", getUserName());

		return model;

	}

	@RequestMapping(value = "/tourUpdate", method = RequestMethod.POST)
	public ModelAndView processFormUpdate(@Valid @ModelAttribute Tour list, BindingResult bindingResult
			, Principal principal) throws SQLException {
		ModelAndView model = new ModelAndView("redirect:/user/tourRetrieveAll");
		
		if(bindingResult.hasErrors()){
			model = new ModelAndView("Tour/tourUpdate");
			
			return model;
		}
		if (!("").equals(list.getPhotoFile().getOriginalFilename())) {
			storageService.delete(dao.findOne(list.getId()).getPhoto());
			storageService.store(list.getPhotoFile());
			list.setPhoto();
		}
		list.setAccountTour(memberDao.findOne(principal.getName()));
		dao.save(list);
		
		return model;
	}

	@RequestMapping(value = "/tourDelete", method = RequestMethod.GET)
	public ModelAndView deleteTour(
			@RequestParam(value = "id", required = false, defaultValue = "1") Long id) {
		ModelAndView model = new ModelAndView("redirect:/user/tourRetrieveAll");
		Tour tour = dao.findOne(id);
		storageService.delete(tour.getPhoto());
		dao.delete(tour);
		
		return model;
	}
	
	public String getUserName(){
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentUserName = authentication.getName();
		String name = memberDao.findOne(currentUserName).getName();
		
		return name;
	}

}
