package aposta.register.euro.resources;


import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import aposta.register.euro.entities.CreditData;
import aposta.register.euro.entities.DigitalCheck;
import aposta.register.euro.grpc.ClientEuromil;
import aposta.register.euro.service.DigitalCheckService;
import aposta.register.euro.utils.FromStringTpDate;
import aposta.register.euro.utils.Valid;


@Controller
public class ApiClienteResource {
    
    private String checkID;

    // Metodo GET para o endpoint /creditBank, que devolve a primeira View para o utilizador 
    // introduzir os dados da conta e o montante
    @GetMapping("/creditBank")
    public String exibirFormulario() {
        return "formulario";
    }


    // Método Get para o endpoint "/submeter" que é chamado quando o utilizador submete os dados da conta e do montante
    // esses dados vêm por método GET no URL
    // Apos isso, esses dados vão ser validados, se estão nos formatos corretos e só depois e chamado o método à classe
    // DigitalCheckService para fazer o pedido GET à API REST
    @GetMapping("/submeter")
    public String submeterFormulario(@RequestParam String credit_account_id,
                                     @RequestParam String ammount,
                                     Model model) {

        // Verificar se os valores introduzidos respeitam as condiçoes para os valores
        // credita_account_id --> 8 digitos decimais
        boolean checkAccount = Valid.checkCreditAccountID(credit_account_id);
        if (checkAccount == false)
        {
            // caso não Esteja correto o credit_account_id, mostra uma mensagem de informação!
            // e devolve novamente a view para introduzirem novamente os dados
            model.addAttribute("mensagem_credit_account", "Account ID must be 8 digits!");
            return "formulario";
        }


        // Verificar se o ammount é um numero, se é superior a 0 e se é igual a 10;
        // Pois na definição é dito que é para criar um cheque de 10 creditos 
        boolean checkAmmount = Valid.checkAmmount(ammount);
        if (checkAmmount == false)
        {
              // caso não Esteja correto o amount mostra uma mensagem de informação!
               // e devolve novamente a view para introduzirem novamente os dados
              model.addAttribute("mensagem_ammount", "The Ammount must be 10 credits");
              return "formulario";
        }
        
        // Fazer uma Requesição à API REST"
        // 1º Criar um objeto com os dados de entrada
        CreditData creditData = new CreditData(Long.parseLong(credit_account_id), Integer.parseInt(ammount));

        // 2º Defenir a String com o URL para o Request À API REST
        String url = "http://ken.utad.pt:8181/check/";


        // 3º Chamar o Service para fazer o pedido com o URL obtido e guardar a resposta em um objeto DigitalCheck
        DigitalCheck digitalCheck = DigitalCheckService.getDigitalCheck(url, creditData);

        // Verificar se o digitalCheck é diferente de null;
        if (digitalCheck!= null)
        {
            String data = FromStringTpDate.convertDateToString(digitalCheck.getCheckDate());
            String checkID = Long.toString(digitalCheck.getCheckId());
        
            // Isto é apenas para mostras as mensagens no html da View se a resposta for diferente de null
            model.addAttribute("checkmessage", "CheckID Complete with success!");
            model.addAttribute("data", data);
            model.addAttribute("checkid", checkID);
            this.checkID = checkID;
            return "submitEuro";
        }

        // se forn null, devolve outra mensagem para a View 
        // mostra novamente a view para introduzir novamente os dados
        model.addAttribute("mensagem_ammount", "It is not possible generate the Check. Please try again!");
        return "formulario";
    }


    // Vai mostrar uma View HTML com as janelas para introduzir os numeros para a chave do Euromil 
    @GetMapping("/euromil_register")
    public String paginaB( @RequestParam("checkid") String valor, Model model) {
      
        model.addAttribute("checkid", checkID);
        return "euromil_register";
        
    }

    // quando fizer o submit no botão do formulário, vem para este POST com os valores todos para juntar 
    // na chave do Euromil e enviar por grpc para o Servidor para registar a aposta!
    @PostMapping("/euromil_register")
    public String postMethodName(//@RequestParam("checkID") String checkID,
                                    @RequestParam("val1") String val1,
                                    @RequestParam("val2") String val2,
                                    @RequestParam("val3") String val3,
                                    @RequestParam("val4") String val4,
                                    @RequestParam("val5") String val5,
                                    @RequestParam("star1") String star1,
                                    @RequestParam("star2") String star2,
                                    Model model)  
    {
        // Fazer o pedido gRPC
        // 1º Adicionar os parametros a uma lista
        List<String> list = new ArrayList<String>();
        list.add(val1);
        list.add(val2);
        list.add(val3);
        list.add(val4);
        list.add(val5);

        // 2º Verifica se contem numeros Repetidos, se são todos numeros 
        boolean result = Valid.checkUniqueValues(list);
        if (result == false)
        {
            // Se detetar alguma irregularidade, devolve novamente a mesma view para introduzir os numeros, junto da mensagem de erro
            model.addAttribute("message_number", "Some invalid number! The number need to be Unique!");
             return "euromil_register";
        }

        // 3º Adicionar as estrelas a uma outra lista
        List<String> nstr = new ArrayList<String>();
        nstr.add(star1);
        nstr.add(star2);

        // 4º Verifica se contem numeros Repetidos, se são todos numeros
         result = Valid.checkUniqueValues(nstr);
         if (result == false)
         {
            // Se detetar alguma irregularidade, devolve novamente a mesma view para introduzir os numeros, junto da mensagem de erro
             model.addAttribute("message_number", "Some invalid number! The number need to be unique!");
              return "euromil_register";
         }
     
        // 5º Verifica se são todos numeros entre 1 e 50 
        result = Valid.euroNumber(list, 50);
        if (result == false)
        {
            // Se detetar alguma irregularidade, devolve novamente a mesma view para introduzir os numeros, junto da mensagem de erro
            model.addAttribute("message_number", "Some invalid number! The number need to be between 1 - 50");
             return "euromil_register";
        }

        // 6º Verifica se as estrelas são numeros entre 1 e 9
        result = Valid.euroNumber(nstr, 9);
        if (result == false)
        {
            // Se detetar alguma irregularidade, devolve novamente a mesma view para introduzir os numeros, junto da mensagem de erro
            model.addAttribute("message_number", "Some invalid number! The Stars need to be between 1 - 5");
             return "euromil_register";
        }

        // 7º cria uma lista unica para criar uma string com todos os numeros da chave
        list.add(nstr.get(0));
        list.add(nstr.get(1));

        // 8º Chama um método para criar a String com todos os valores no formato que o Servidor gRPC exige
        String Key = Valid.createString(list);

        // 9º Cria um objeto da classe que implementa o método do serviço gRPC, permitindo que o construtor já crie o channel e o Stub
        ClientEuromil client = new ClientEuromil();
        String resultMessage = " ";

        // 10º Faz a Requesição gRPC ao Servidor
        try{
            resultMessage = client.registerEuroMil(Key, this.checkID);
            System.out.println("Result: " + result);
        }catch (Exception e){
            System.out.println("Some error ocurred in the Server: " + e.getMessage());
            resultMessage = "Error to connect to the Server to get the Check";
        }
        // devolve a Resposta obtida no gRPC para uma View
        model.addAttribute("mensagem", resultMessage);
        model.addAttribute("checkID", checkID);
       return "result";
    }

}


