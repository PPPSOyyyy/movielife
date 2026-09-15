package com.yse.dev.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.yse.dev.DTO.PersonDetailDto;
import com.yse.dev.Service.PersonService;


@Controller
public class PersonController {


    private final PersonService personService;


    public PersonController(
            PersonService personService) {

        this.personService =
                personService;
    }


    // =========================================================
    // 배우 상세
    // =========================================================

    @GetMapping("/people/{personId}")
    public String personDetail(

            @PathVariable("personId")
            Long personId,

            Model model) {


        PersonDetailDto person =
                personService.getPersonDetail(
                        personId
                );


        model.addAttribute(
                "person",
                person
        );


        return "person-detail";
    }
}