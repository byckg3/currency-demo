package demo.currency.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import demo.currency.model.exception.CurrencyAlreadyExistsException;
import demo.currency.model.exception.CurrencyNotFoundException;
import demo.currency.model.exception.Error;
import demo.currency.model.repository.Currency;
import demo.currency.model.service.CurrencyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping( path = "/currencies" )
@CrossOrigin( origins = "*" )
public class CurrencyController
{
    private CurrencyService currencyService;
    
    public CurrencyController( CurrencyService currService ) {
        this.currencyService = currService;
    }

    @GetMapping()
    public List< Currency > getAll() {
        return currencyService.getAll();
    }

    @GetMapping( "/{code}" )
    @ResponseStatus( HttpStatus.OK )
    public Currency get( @PathVariable( "code" ) String code )
    {
        Optional< Currency > foundCurrency = currencyService.findByCode( code );
        if( !foundCurrency.isPresent() ) {
            throw new CurrencyNotFoundException( code );
        }
        return foundCurrency.get();
    }

    @PostMapping( consumes = "application/json" )
    public ResponseEntity< Currency > create( @RequestBody Currency curr, UriComponentsBuilder ucb )
    {
        if ( currencyService.currencyExists( curr.getCode() ) ) {
            throw new CurrencyAlreadyExistsException( curr.getCode(), curr.getName() );
        }
        
        Currency currency = currencyService.save( curr );

        URI locationUri = ucb.path( "/currencies/" )
                             .path( currency.getCode() )
                             .build()
                             .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation( locationUri );

        return new ResponseEntity< Currency >( currency, headers, HttpStatus.CREATED );
    }

    @PatchMapping( path = "/{code}", consumes = "application/json" )
    public Currency update( @PathVariable( "code" ) String code, @RequestBody Currency patch )
    {
        return currencyService.updateByCode( code, patch );
    }

    @DeleteMapping( "/{code}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void delete( @PathVariable( "code" ) String codeName )
    {
        try {
            currencyService.deleteByCode( codeName );
        }
        catch( EmptyResultDataAccessException e ) {
            System.out.println("codeName" + codeName );
            log.error( codeName + " " + e.getMessage() );
        }
    }

    @ExceptionHandler( CurrencyNotFoundException.class )
    @ResponseStatus( HttpStatus.NOT_FOUND )
    public Error handleJsonException( CurrencyNotFoundException e )
    {
        log.error( e.getMessage() );
        return new Error( "currency " + e.getCurrencyCode() + " not found" );
    }

    @ExceptionHandler( CurrencyAlreadyExistsException.class )
    @ResponseStatus( HttpStatus.CONFLICT )
    public Error handleJsonException( CurrencyAlreadyExistsException e )
    {
        log.error( e.getMessage() );
        return new Error( "currency " + e.getCurrencyCode() + " already exists" );
    }
}
