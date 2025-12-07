package com.vexa.vantage.controller;

import com.vexa.vantage.model.Contact;
import com.vexa.vantage.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping("/send")
    public ResponseEntity<?> sendContactMessage(@RequestBody Contact contact) {
        contactService.saveContact(contact);
        return ResponseEntity.ok("Message sent successfully!");
    }
}
