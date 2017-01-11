
/*!
 ***********************************************************************
 * \file
 *    configfile.c
 * \brief
 *    Configuration handling.
 * \author
 *  Main contributors (see contributors.h for copyright, address and affiliation details)
 *    - Stephan Wenger           <stewe@cs.tu-berlin.de>
 * \note
 *    In the future this module should hide the Parameters and offer only
 *    Functions for their access.  Modules which make frequent use of some parameters
 *    (e.g. picture size in macroblocks) are free to buffer them on local variables.
 *    This will not only avoid global variable and make the code more readable, but also
 *    speed it up.  It will also greatly facilitate future enhancements such as the
 *    handling of different picture sizes in the same sequence.                         \n
 *                                                                                      \n
 *    For now, everything is just copied to the inp_par structure (gulp)
 *
 **************************************************************************************
 * \par Configuration File Format
 **************************************************************************************
 * Format is line oriented, maximum of one parameter per line                           \n
 *                                                                                      \n
 * Lines have the following format:                                                     \n
 * \<ParameterName\> = \<ParameterValue\> # Comments \\n                                    \n
 * Whitespace is space and \\t
 * \par
 * \<ParameterName\> are the predefined names for Parameters and are case sensitive.
 *   See configfile.h for the definition of those names and their mapping to
 *   cfgparams->values.
 * \par
 * \<ParameterValue\> are either integers [0..9]* or strings.
 *   Integers must fit into the wordlengths, signed values are generally assumed.
 *   Strings containing no whitespace characters can be used directly.  Strings containing
 *   whitespace characters are to be inclosed in double quotes ("string with whitespace")
 *   The double quote character is forbidden (may want to implement something smarter here).
 * \par
 * Any Parameters whose ParameterName is undefined lead to the termination of the program
 * with an error message.
 *
 * \par Known bug/Shortcoming:
 *    zero-length strings (i.e. to signal an non-existing file
 *    have to be coded as "".
 *
 * \par Rules for using command files
 *                                                                                      \n
 * All Parameters are initially taken from DEFAULTCONFIGFILENAME, defined in configfile.h.
 * If an -f \<config\> parameter is present in the command line then this file is used to
 * update the defaults of DEFAULTCONFIGFILENAME.  There can be more than one -f parameters
 * present.  If -p <ParameterName = ParameterValue> parameters are present then these
 * override the default and the additional config file's settings, and are themselves
 * overridden by future -p parameters.  There must be whitespace between -f and -p commands
 * and their respective parameters
 ***********************************************************************
 */

#define INCLUDED_BY_CONFIGFILE_C

#include <sys/stat.h>

#include "global.h"
#include "memalloc.h"
#include "config_common.h"
#include "configfile.h"
#define MAX_ITEMS_TO_PARSE  10000

static void PatchInp                (InputParameters *p_Inp);

/*!
 ***********************************************************************
 * \brief
 *   print help message and exit
 ***********************************************************************
 */
void JMDecHelpExit (void)
{
  fprintf( stderr, "\n   ldecod [-h] [-d defdec.cfg] {[-f curenc1.cfg]...[-f curencN.cfg]}"
    " {[-p EncParam1=EncValue1]..[-p EncParamM=EncValueM]}\n\n"
    "## Parameters\n\n"

    "## Options\n"
    "   -h :  prints function usage\n"
    "   -d :  use <defdec.cfg> as default file for parameter initializations.\n"
    "         If not used then file defaults to encoder.cfg in local directory.\n"
    "   -f :  read <curencM.cfg> for reseting selected encoder parameters.\n"
    "         Multiple files could be used that set different parameters\n"
    "   -p :  Set parameter <DecParamM> to <DecValueM>.\n"
    "         See default decoder.cfg file for description of all parameters.\n\n"

    "## Examples of usage:\n"
    "   ldecod\n"
    "   ldecod  -h\n"
    "   ldecod  -d default.cfg\n"
    "   ldecod  -f curenc1.cfg\n"
    "   ldecod  -f curenc1.cfg -p InputFile=\"e:\\data\\container_qcif_30.264\" -p OutputFile=\"dec.yuv\" -p RefFile=\"Rec.yuv\"\n");

  exit(-1);
}


/*!
************************************************************************
* \brief
*    exit with error message if reading from config file failed
************************************************************************
*/
static inline void conf_read_check (int val, int expected)
{
  if (val != expected)
  {
    error ("init_conf: error reading from config file", 500);
  }
}

/*!
 ***********************************************************************
 * \brief
 *    Parse the command line parameters and read the config files.
 * \param p_Vid
 *    VideoParameters structure for encoding
 * \param p_Inp
 *    InputParameters structure as input configuration
 * \param ac
 *    number of command line parameters
 * \param av
 *    command line parameters
 ***********************************************************************
 */
void ParseCommand(InputParameters *p_Inp, int ac, char *av[])
{
  char *content = NULL;
  int CLcount, ContentLen, NumberParams;
  char *filename=DEFAULTCONFIGFILENAME;

  if (ac==2)
  {
    if (0 == strncmp (av[1], "-v", 2))
    {
      printf("JM " JM ": compiled " __DATE__ " " __TIME__ "\n");
      exit(-1);
    }

    if (0 == strncmp (av[1], "-h", 2))
    {
      JMDecHelpExit();
    }
  }

  memcpy (&cfgparams, p_Inp, sizeof (InputParameters));
  //Set default parameters.
  printf ("Setting Default Parameters...\n");
  InitParams(Map);

  *p_Inp = cfgparams;
  // Process default config file
  CLcount = 1;

  if (ac>=3)
  {
    if ((strlen(av[1])==2) && (0 == strncmp (av[1], "-d", 2)))
    {
      if(0 == strncmp (av[2], "null", 4))
        filename=NULL;
      else
        filename=av[2];
      CLcount = 3;
    }
    if (0 == strncmp (av[1], "-h", 2))
    {
      JMDecHelpExit();
    }
  }
  if(filename)
  {
    printf ("Parsing Configfile %s\n", filename);
    content = GetConfigFileContent (filename);
    if (NULL != content)
    {
      //error (errortext, 300);
      ParseContent (p_Inp, Map, content, (int) strlen(content));
      printf ("\n");
      free (content);
    }
  }
  // Parse the command line

  while (CLcount < ac)
  {
    if (0 == strncmp (av[CLcount], "-h", 2))
    {
      JMDecHelpExit();
    }

    if (0 == strncmp (av[CLcount], "-f", 2) || 0 == strncmp (av[CLcount], "-F", 2))  // A file parameter?
    {
      content = GetConfigFileContent (av[CLcount+1]);
      if (NULL==content)
        error (errortext, 300);
      printf ("Parsing Configfile %s", av[CLcount+1]);
      ParseContent (p_Inp, Map, content, (int) strlen (content));
      printf ("\n");
      free (content);
      CLcount += 2;
    } 
    else if (0 == strncmp (av[CLcount], "-i", 2) || 0 == strncmp (av[CLcount], "-I", 2))  // A file parameter?
    {
      strncpy(p_Inp->infile, av[CLcount+1], FILE_NAME_SIZE);
      CLcount += 2;
    } 
    else if (0 == strncmp (av[CLcount], "-r", 2) || 0 == strncmp (av[CLcount], "-R", 2))  // A file parameter?
    {
      strncpy(p_Inp->reffile, av[CLcount+1], FILE_NAME_SIZE);
      CLcount += 2;
    } 
    else if (0 == strncmp (av[CLcount], "-o", 2) || 0 == strncmp (av[CLcount], "-O", 2))  // A file parameter?
    {
      strncpy(p_Inp->outfile, av[CLcount+1], FILE_NAME_SIZE);
      CLcount += 2;
    } 
    else if (0 == strncmp (av[CLcount], "-s", 2) || 0 == strncmp (av[CLcount], "-S", 2))  // A file parameter?
    {
      p_Inp->silent = 1;
      CLcount += 1;
    }
    else if (0 == strncmp (av[CLcount], "-n", 2) || 0 == strncmp (av[CLcount], "-N", 2))  // A file parameter?
    {
      conf_read_check (sscanf(av[CLcount+1],"%d", &p_Inp->iDecFrmNum), 1);
      CLcount += 2;
    }
#if (MVC_EXTENSION_ENABLE)
    else if (0 == strncmp (av[CLcount], "-mpr", 4) || 0 == strncmp (av[CLcount], "-MPR", 4))  // A file parameter?
    {
      conf_read_check (sscanf(av[CLcount+1],"%d", &p_Inp->DecodeAllLayers), 1);
      CLcount += 2;
    } 
#endif
    else if (0 == strncmp (av[CLcount], "-p", 2) || 0 == strncmp (av[CLcount], "-P", 2))  // A config change?
    {
      // Collect all data until next parameter (starting with -<x> (x is any character)),
      // put it into content, and parse content.

      ++CLcount;
      ContentLen = 0;
      NumberParams = CLcount;

      // determine the necessary size for content
      while (NumberParams < ac && av[NumberParams][0] != '-')
        ContentLen += (int) strlen (av[NumberParams++]);        // Space for all the strings
      ContentLen += 1000;                     // Additional 1000 bytes for spaces and \0s


      if ((content = malloc (ContentLen))==NULL) no_mem_exit("Configure: content");;
      content[0] = '\0';

      // concatenate all parameters identified before

      while (CLcount < NumberParams)
      {
        char *source = &av[CLcount][0];
        char *destin = &content[(int) strlen (content)];

        while (*source != '\0')
        {
          if (*source == '=')  // The Parser expects whitespace before and after '='
          {
            *destin++=' '; *destin++='='; *destin++=' ';  // Hence make sure we add it
          } 
          else
            *destin++=*source;
          source++;
        }
        *destin = '\0';
        CLcount++;
      }
      printf ("Parsing command line string '%s'", content);
      ParseContent (p_Inp, Map, content, (int) strlen(content));
      free (content);
      printf ("\n");
    }
    else
    {
      snprintf (errortext, ET_SIZE, "Error in command line, ac %d, around string '%s', missing -f or -p parameters?", CLcount, av[CLcount]);
      error (errortext, 300);
    }
  }
  printf ("\n");

  PatchInp(p_Inp);
  cfgparams = *p_Inp;
  p_Inp->enable_32_pulldown = 0;
  if (p_Inp->bDisplayDecParams)
    DisplayParams(Map, "Decoder Parameters");
}


/*!
 ***********************************************************************
 * \brief
 *    Checks the input parameters for consistency.
 ***********************************************************************
 */
static void PatchInp (InputParameters *p_Inp)
{
  //int i;
  //int storedBplus1;
  TestParams(Map, NULL);
  if(p_Inp->export_views == 1)
    p_Inp->dpb_plus[1] = imax(1, p_Inp->dpb_plus[1]);
}

