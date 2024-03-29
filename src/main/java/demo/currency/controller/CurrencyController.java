package demo.currency.controller;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import demo.currency.config.AppProperties;
import demo.currency.model.Currency;
import demo.currency.model.exception.CurrencyAlreadyExistsException;
import demo.currency.model.exception.CurrencyNotFoundException;
import demo.currency.model.exception.Error;
import demo.currency.model.service.CurrencyService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping( path = "/currencies" )
@CrossOrigin( origins = "*" )
public class CurrencyController
{
    private AppProperties appProps;
    private CurrencyService currencyService;
    
    @Autowired
    public CurrencyController( CurrencyService currService, AppProperties props ) {
        this.currencyService = currService;
        this.appProps = props;
    }

    @GetMapping()
    public List< Currency > getAll( @RequestParam Optional< Integer > page, @RequestParam Optional< Integer > size )
    {
        var pageRequest = PageRequest.of( page.orElse( 0 ),
                                          size.orElse( appProps.getPageSize() ),
                                          Sort.by( "createdDate" ).descending() );
        
        return currencyService.getAll( pageRequest ).getContent();
    }

    @GetMapping( "/{code}" )
    @ResponseStatus( HttpStatus.OK )
    public Currency getByCode( @PathVariable( "code" ) String code )
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
    public Currency patchByCode( @PathVariable( "code" ) String code, @RequestBody Currency patch )
    {
        return currencyService.updateByCode( code, patch );
    }

    @PutMapping( path = "/{code}", consumes = "application/json" )
    public Currency updateByCode( @PathVariable( "code" ) String code, @RequestBody Currency patch )
    {
        return patchByCode( code, patch );
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
    public Error handleCurrencyException( CurrencyNotFoundException e )
    {
        log.error( e.getMessage() );
        return new Error( "currency " + e.getCurrencyCode() + " not found" );
    }

    @ExceptionHandler( CurrencyAlreadyExistsException.class )
    @ResponseStatus( HttpStatus.CONFLICT )
    public Error handleCurrencyException( CurrencyAlreadyExistsException e )
    {
        log.error( e.getMessage() );
        return new Error( "currency " + e.getCurrencyCode() + " already exists" );
    }

    @ExceptionHandler( Exception.class )
    @ResponseStatus( HttpStatus.INTERNAL_SERVER_ERROR )
    public String handleException( Exception e )
    {
        log.error( e.getMessage() );
        return "{ msg: \"error occurs\" }";
    }
}
