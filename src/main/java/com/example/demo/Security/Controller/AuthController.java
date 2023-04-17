//Controlador que se va a comunicar con el FRONT

package com.example.demo.Security.Controller;

import com.example.demo.Security.Dto.JwtDto;
import com.example.demo.Security.Dto.LoginUsuario;
import com.example.demo.Security.Dto.NuevoUsuario;
import com.example.demo.Security.Entity.Rol;
import com.example.demo.Security.Entity.Usuario;
import com.example.demo.Security.Enums.RolNombre;
import com.example.demo.Security.Service.RolService;
import com.example.demo.Security.Service.UsuarioService;
import com.example.demo.Security.jwt.JwtProvider;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UsuarioService usuarioService;
    @Autowired
    RolService rolService;
    @Autowired
    JwtProvider jwtProvider;
    
    //Crea un Usuario nuevo
    @PostMapping("/nuevo")
    public ResponseEntity<?> nuevo(@Valid @RequestBody NuevoUsuario nuevoUsuario, BindingResult bindinResult){
        if(bindinResult.hasErrors())
            return new ResponseEntity(new Mensaje("Campos mal puesto o email invalido"), HttpStatus.BAD_REQUEST);
        
        if(usuarioService.existByNombreUsuario(nuevoUsuario.getNombreUsuario()))
            return new ResponseEntity(new Mensaje("Ese nombre de usuario ya existe"), HttpStatus.BAD_REQUEST);
        
        if(usuarioService.existByEmail(nuevoUsuario.getEmail()))
            return new ResponseEntity(new Mensaje("Ese email de ya existe"), HttpStatus.BAD_REQUEST);
        
        Usuario usuario = new Usuario(nuevoUsuario.getNombre(), nuevoUsuario.getNombreUsuario(),
                            nuevoUsuario.getEmail(), passwordEncoder.encode(nuevoUsuario.getPassword()));
        
        Set<Rol> roles = new HashSet<>();
        roles.add(rolService.getByRolNombre(RolNombre.ROLE_USER).get());
        
        if(nuevoUsuario.getRoles().contains("admin"))
            roles.add(rolService.getByRolNombre(RolNombre.ROLE_ADMIN).get());
        usuario.setRoles(roles);
        usuarioService.save(usuario);
        
        return new ResponseEntity(new Mensaje("Usuario guardado"), HttpStatus.CREATED);
    }
    
    @PostMapping("/login")
    public ResponseEntity<JwtDto> login(@Valid @RequestBody LoginUsuario loginUsuario, BindingResult bindingResult){
        if(bindingResult.hasErrors())
                return new ResponseEntity(new Mensaje("Campos mal puestos"), HttpStatus.BAD_REQUEST);
        
          Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                       loginUsuario.getNombreUsuario(), loginUsuario.getPassword()));
          
          SecurityContextHolder.getContext().setAuthentication(authentication);
          
          String jwt = jwtProvider.generateToken(authentication);
          
          UserDetails userDetails = (UserDetails) authentication.getPrincipal();
          
          JwtDto jwtDto = new JwtDto(jwt, userDetails.getUsername(), userDetails.getAuthorities());
          
          return new ResponseEntity(jwtDto, HttpStatus.OK);
    }
}
